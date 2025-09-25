#!/usr/bin/env bash
set -Eeuo pipefail

# setup_zscaler --jdk <PATH_TO_JDK> --project <PATH_TO_ANDROID_PROJECT>

die() { echo "❌ $*" >&2; exit 1; }
info() { echo "➡️  $*"; }
ok()   { echo "✅ $*"; }

JDK_INPUT=""
PROJECT_ROOT=""

# ------- Parse args -------
while [[ $# -gt 0 ]]; do
  case "$1" in
    --jdk) JDK_INPUT="$2"; shift 2 ;;
    --project) PROJECT_ROOT="$2"; shift 2 ;;
    *) die "Unknown argument: $1 (expected --jdk or --project)" ;;
  esac
done

[[ -n "$JDK_INPUT" ]] || die "--jdk <PATH_TO_JDK> is required"
[[ -n "$PROJECT_ROOT" ]] || die "--project <PATH_TO_ANDROID_PROJECT> is required"
[[ -d "$JDK_INPUT" ]] || die "JDK path not found: $JDK_INPUT"
[[ -d "$PROJECT_ROOT" ]] || die "Android project path not found: $PROJECT_ROOT"

# ------- Locate truststore -------
CANDIDATES=(
  "$JDK_INPUT/lib/security/cacerts"
  "$JDK_INPUT/Contents/Home/lib/security/cacerts"
)
TRUSTSTORE=""
for c in "${CANDIDATES[@]}"; do
  [[ -f "$c" ]] && { TRUSTSTORE="$c"; break; }
done
[[ -n "$TRUSTSTORE" ]] || die "Could not find cacerts under '$JDK_INPUT'. Tried: ${CANDIDATES[*]}"

# ------- Locate project files -------
GRADLE_PROPS="$PROJECT_ROOT/gradle.properties"
[[ -f "$GRADLE_PROPS" ]] || { info "Creating missing gradle.properties at project root"; : > "$GRADLE_PROPS"; }

MANIFEST_PATH="$(find "$PROJECT_ROOT" -type f -path "*/src/main/AndroidManifest.xml" | head -n 1 || true)"
[[ -n "$MANIFEST_PATH" ]] || die "Could not find any */src/main/AndroidManifest.xml in $PROJECT_ROOT"

# FIX: go up TWO levels from .../src/main/AndroidManifest.xml to module root (.../app)
MODULE_DIR="$(cd "$(dirname "$MANIFEST_PATH")/../.." && pwd)"
RES_XML_DIR="$MODULE_DIR/src/main/res/xml"
NETWORK_XML="$RES_XML_DIR/network_security_config.xml"

info "Using Android module: $MODULE_DIR"
info "Manifest: $MANIFEST_PATH"

# ------- 1) Update gradle.properties -------
info "Updating gradle.properties…"
cp -p "$GRADLE_PROPS" "$GRADLE_PROPS.bak.$(date +%Y%m%d-%H%M%S)"

upsert_prop() {
  local key="$1" value="$2"
  if grep -qE "^[[:space:]]*${key}[[:space:]]*=" "$GRADLE_PROPS"; then
    # POSIX sed for macOS/Linux
    sed -i'' -E "s|^[[:space:]]*${key}[[:space:]]*=.*|${key}=${value}|" "$GRADLE_PROPS"
  else
    printf "%s=%s\n" "$key" "$value" >> "$GRADLE_PROPS"
  fi
}

upsert_prop "systemProp.javax.net.ssl.trustStore" "$TRUSTSTORE"
upsert_prop "systemProp.javax.net.ssl.trustStorePassword" "changeit"
upsert_prop "systemProp.com.sun.net.ssl.checkRevocation" "false"
upsert_prop "systemProp.sun.security.ssl.allowUnsafeRenegotiation" "true"
upsert_prop "systemProp.sun.security.ssl.allowLegacyHelloMessages" "true"

ok "gradle.properties updated."

# ------- 2) Create network_security_config.xml -------
info "Ensuring res/xml/network_security_config.xml…"
mkdir -p "$RES_XML_DIR"
cat > "$NETWORK_XML" <<'XML'
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <!-- Allow cleartext traffic for localhost, emulator, and webhook.site -->
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">10.0.2.2</domain>
        <domain includeSubdomains="true">localhost</domain>
        <domain includeSubdomains="true">127.0.0.1</domain>
        <domain includeSubdomains="true">bugsnag.com</domain>
    </domain-config>

    <!-- Base configuration -->
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system"/>
            <certificates src="user"/>
        </trust-anchors>
    </base-config>

    <!-- Debug overrides - accept all certificates including self-signed -->
    <debug-overrides>
        <trust-anchors>
            <certificates src="system"/>
            <certificates src="user"/>
            <!-- This allows self-signed certificates in debug builds -->
        </trust-anchors>
    </debug-overrides>
</network-security-config>
XML
ok "Created: $NETWORK_XML"

# ------- 3) Patch AndroidManifest.xml -------
info "Patching AndroidManifest.xml…"
cp -p "$MANIFEST_PATH" "$MANIFEST_PATH.bak.$(date +%Y%m%d-%H%M%S)"

# Only add attribute if not present; portable via perl (macOS has perl)
if ! grep -q 'android:networkSecurityConfig=' "$MANIFEST_PATH"; then
  perl -0777 -i -pe 's/<application\b(?![^>]*android:networkSecurityConfig=)/<application android:networkSecurityConfig="@xml\/network_security_config"/s' "$MANIFEST_PATH"
fi

# Upsert Bugsnag endpoints (HTTP) just before </application>
TMP_MANIFEST="$(mktemp)"
# Remove any existing occurrences of these meta-data lines (simple line-based filter)
sed -E '/<meta-data[[:space:]]+android:name="com\.bugsnag\.android\.ENDPOINT_NOTIFY"/d' "$MANIFEST_PATH" \
  | sed -E '/<meta-data[[:space:]]+android:name="com\.bugsnag\.android\.ENDPOINT_SESSIONS"/d' \
  > "$TMP_MANIFEST"

awk '
  BEGIN {
    notify = "        <meta-data android:name=\"com.bugsnag.android.ENDPOINT_NOTIFY\"\n" \
             "            android:value=\"http://notify.bugsnag.com\"/>\n"
    sessions = "        <meta-data android:name=\"com.bugsnag.android.ENDPOINT_SESSIONS\"\n" \
               "            android:value=\"http://sessions.bugsnag.com\"/>\n"
  }
  /<\/application>/ && ! inserted { print notify sessions; inserted=1 }
  { print }
' "$TMP_MANIFEST" > "$MANIFEST_PATH"
rm -f "$TMP_MANIFEST"

ok "Manifest patched."

echo
ok "All done."
echo "   Project root : $PROJECT_ROOT"
echo "   Module dir   : $MODULE_DIR"
echo "   Manifest     : $MANIFEST_PATH"
echo "   Network XML  : $NETWORK_XML"
echo "   Truststore   : $TRUSTSTORE"