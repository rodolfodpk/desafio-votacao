#!/bin/bash

# Generate coverage badge locally
echo "Generating coverage badge..."

# Run tests and generate coverage report
./mvnw clean test jacoco:report --batch-mode

# Extract coverage percentage from JaCoCo report
COVERAGE_PERCENT=$(grep -o 'tfoot.*<td class="ctr2">[0-9]*%</td>' target/site/jacoco/index.html | grep -o '[0-9]*%' | head -1)

if [ -z "$COVERAGE_PERCENT" ]; then
    echo "Could not extract coverage percentage"
    exit 1
fi

# Remove % sign for badge generation
COVERAGE_NUMBER=${COVERAGE_PERCENT%?}

# Determine badge color based on coverage
if [ "$COVERAGE_NUMBER" -ge 90 ]; then
    COLOR="brightgreen"
elif [ "$COVERAGE_NUMBER" -ge 80 ]; then
    COLOR="green"
elif [ "$COVERAGE_NUMBER" -ge 70 ]; then
    COLOR="yellow"
elif [ "$COVERAGE_NUMBER" -ge 60 ]; then
    COLOR="orange"
else
    COLOR="red"
fi

# Generate badge URL
BADGE_URL="https://img.shields.io/badge/coverage-${COVERAGE_PERCENT}-${COLOR}.svg"

echo "Coverage: $COVERAGE_PERCENT"
echo "Badge URL: $BADGE_URL"
echo "Badge Markdown: [![Coverage]($BADGE_URL)](target/site/jacoco/index.html)"

# Create a simple HTML file with the badge
cat > coverage-badge.html << EOF
<!DOCTYPE html>
<html>
<head>
    <title>Test Coverage Badge</title>
</head>
<body>
    <h1>Test Coverage: $COVERAGE_PERCENT</h1>
    <img src="$BADGE_URL" alt="Coverage: $COVERAGE_PERCENT">
    <p><a href="target/site/jacoco/index.html">View detailed coverage report</a></p>
</body>
</html>
EOF

echo "Coverage badge generated in coverage-badge.html"
echo "Open coverage-badge.html in your browser to view the badge"
