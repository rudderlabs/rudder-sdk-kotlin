#!/bin/bash

# Script to update integration module build.gradle.kts files to include version in AAR filenames
# This script adds dynamic AAR filename configuration using RudderStackBuildConfig.Integrations.getByModuleName()
# Usage: ./scripts/update-integration-aar-filenames.sh

set -e

echo "🔧 Updating integration modules to include version in AAR filenames using dynamic approach..."

# List of integration modules
INTEGRATION_MODULES=(
    "adjust"
    "braze"
    "facebook"
    "firebase"
)

# Function to update a single integration module
update_integration_module() {
    local module="$1"
    local build_file="integrations/$module/build.gradle.kts"
    
    if [ ! -f "$build_file" ]; then
        echo "⚠️  Build file not found: $build_file"
        return 1
    fi
    
    echo "Updating $module..."
    
    # Check if the dynamic AAR filename configuration already exists
    if grep -q "RudderStackBuildConfig.Integrations.getByModuleName" "$build_file"; then
        echo "✅ $module already has dynamic AAR filename configuration"
        return 0
    fi
    
    # Find the buildTypes block and add the dynamic configuration after it
    if grep -q "buildTypes {" "$build_file"; then
        # Create a temporary file with the updated content
        awk '
        /^    buildTypes \{/ {
            print $0
            in_build_types = 1
            brace_count = 1
            next
        }
        in_build_types && /\{/ {
            brace_count++
            print $0
            next
        }
        in_build_types && /\}/ {
            brace_count--
            print $0
            if (brace_count == 0) {
                print ""
                print "    // Configure AAR filename to include version"
                print "    libraryVariants.all {"
                print "        outputs.all {"
                print "            val outputImpl = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl"
                print "            val integrationModule = RudderStackBuildConfig.Integrations.getByModuleName(project.name)"
                print "            outputImpl.outputFileName = \"${project.name}-${buildType.name}-${integrationModule.versionName}.aar\""
                print "        }"
                print "    }"
                in_build_types = 0
            }
            next
        }
        { print $0 }
        ' "$build_file" > "${build_file}.tmp"

        # Replace the original file
        mv "${build_file}.tmp" "$build_file"
        echo "✅ Updated $module"
    else
        echo "❌ Could not find buildTypes block in $build_file"
        return 1
    fi
}

# Update all integration modules
for module in "${INTEGRATION_MODULES[@]}"; do
    update_integration_module "$module"
done

echo ""
echo "✅ All integration modules updated successfully!"
echo ""
echo "The integration modules now use dynamic AAR filename configuration that:"
echo "- Determines integration info from project name using RudderStackBuildConfig.Integrations.getByModuleName()"
echo "- Avoids hardcoded integration names in build scripts"
echo "- Automatically includes version numbers in AAR filenames"
echo ""
echo "Next steps:"
echo "1. Test the build to ensure AAR files have correct names"
echo "2. Verify that new integrations will work automatically without script changes"
