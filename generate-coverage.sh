#!/bin/bash

# Generate test coverage report
echo "🧪 Generating test coverage report..."

# Run tests with coverage
./mvnw test jacoco:report --batch-mode

if [ $? -eq 0 ]; then
    echo "✅ Coverage report generated successfully!"
    
    # Calculate coverage percentage
    if [ -f "target/site/jacoco/jacoco.csv" ]; then
        COVERAGE=$(awk -F',' 'NR>1 {total_covered+=$5; total_missed+=$4} END {printf "%.1f", (total_covered/(total_covered+total_missed))*100}' target/site/jacoco/jacoco.csv)
        echo "📊 Line Coverage: ${COVERAGE}%"
    fi
    
    # Open coverage report
    echo "🌐 Opening coverage report..."
    open target/site/jacoco/index.html
    
    echo ""
    echo "📋 Coverage files generated:"
    echo "   📄 HTML Report: target/site/jacoco/index.html"
    echo "   📊 XML Report: target/site/jacoco/jacoco.xml"
    echo "   📈 CSV Report: target/site/jacoco/jacoco.csv"
    echo ""
    echo "💡 The coverage report will open in your default browser."
else
    echo "❌ Failed to generate coverage report"
    exit 1
fi
