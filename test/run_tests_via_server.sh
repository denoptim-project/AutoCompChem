#!/bin/bash

# AutoCompChem REST API Test Runner
# Processes all JSON test files and executes them against the server

BASE_URL="http://localhost:8080"
TEST_DIR="."
RESULTS_DIR="results_from_sever"
JSONSUFFIX=".srv.json"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to show usage
show_usage() {
    echo "Usage: $0 [TEST_NAME]"
    echo ""
    echo "AutoCompChem REST API Test Runner"
    echo ""
    echo "Arguments:"
    echo "  TEST_NAME    Optional. Run a specific test (without .srv.json extension)"
    echo "               If not provided, runs all tests in the directory"
    echo ""
    echo "Examples:"
    echo "  $0           # Run all tests"
    echo "  $0 t150      # Run only the t150 test"
    echo "  $0 t2        # Run only the t2 test"
    echo ""
    echo "Options:"
    echo "  -h, --help   Show this help message"
}

# Function to convert relative paths to absolute paths in JSON
convert_paths_to_absolute() {
    local params="$1"
    local test_dir="$2"
    
    # Use jq to process the JSON and convert relative paths to absolute
    echo "$params" | jq --arg testdir "$test_dir" --arg resultsdir "$(realpath "$RESULTS_DIR")" '
        # Process each key-value pair directly (params is the object, not wrapped in params)
        to_entries | map(
            if .key == "outFile" then
                # Handle outFile - place in results directory
                if (.value | type) == "string" then
                    if (.value | test("^/")) then
                        .value = .value
                    else
                        .value = "\($resultsdir)/" + .value
                    end
                else
                    .
                end
            elif (.value | type) == "string" and 
                 ((.value | startswith("../")) or (.value | startswith("./")) or
                  ((.value | test("\\.(sdf|xyz|mol|pdb|txt|out|log|dat|inp|json|xml)$")) and (.value | test("^[^/]") or (.value | startswith("../"))))) then
                # Handle other file paths - convert relative to absolute
                if (.value | startswith("../")) then
                    .value = "\($testdir)/" + (.value | sub("^\\.\\./"; ""))
                elif (.value | startswith("./")) then
                    .value = "\($testdir)/" + (.value | sub("^\\./"; ""))
                else
                    .value = "\($testdir)/" + .value
                end
            else
                .
            end
        ) | from_entries
    '
}

# Function to run a single test
run_single_test() {
    local json_file="$1"
    local test_name="$2"
    
    echo -e "${BLUE}🔄 Running test: $test_name${NC}"
    
    # Extract task and params from JSON file
    task=$(jq -r '.task' "$json_file")
    original_params=$(jq -c '.params' "$json_file")
    
    # Check if extraction was successful
    if [ "$task" = "null" ] || [ "$original_params" = "null" ]; then
        echo -e "${RED}❌ Failed to extract task or params from $json_file${NC}"
        return 1
    fi
    
    # Convert relative paths to absolute paths
    # Get the directory containing the JSON file for path resolution
    json_dir=$(dirname "$(realpath "$json_file")")
    params=$(convert_paths_to_absolute "$original_params" "$json_dir")
    
    # Construct API URL
    api_url="$BASE_URL/api/v1/autocompchem/tasks/$task/execute"
    
    echo "   📋 Task: $task"
    echo "   🔗 URL: $api_url"
    
    # Show if paths were converted (for debugging)
    if [ "$original_params" != "$params" ]; then
        echo "   🔄 Converted relative paths to absolute"
        echo "   📋 Sending JSON: $params"
    fi
    
    # Make the curl request and capture response
    response_file="$RESULTS_DIR/${test_name}_response.json"
    
    # Execute curl command with timeout
    http_code=$(curl -s -w "%{http_code}" \
        -X POST \
        -H "Content-Type: application/json" \
        -d "$params" \
        "$api_url" \
        --max-time 30 \
        -o "$response_file")
    
    # Check the HTTP response code
    if [ "$http_code" = "200" ]; then
        echo -e "   ${GREEN}✅ Success (HTTP $http_code)${NC}"
        
        # Try to extract status from response if available
        if [ -f "$response_file" ] && command -v jq &> /dev/null; then
            status=$(jq -r '.status // "unknown"' "$response_file" 2>/dev/null)
            if [ "$status" != "unknown" ] && [ "$status" != "null" ]; then
                echo "   📊 Status: $status"
            fi
        fi
        return 0
    else
        echo -e "   ${RED}❌ Failed (HTTP $http_code)${NC}"
        
        # Show error details if available
        if [ -f "$response_file" ]; then
            echo "   📄 Response saved to: $response_file"
            # Try to show error message if it's JSON
            if command -v jq &> /dev/null; then
                error_msg=$(jq -r '.message // .error // "No error message"' "$response_file" 2>/dev/null)
                if [ "$error_msg" != "null" ] && [ "$error_msg" != "No error message" ]; then
                    echo "   💬 Error: $error_msg"
                fi
            fi
        fi
        return 1
    fi
}

# Parse command line arguments
if [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
    show_usage
    exit 0
fi

# Check for specific test argument
SPECIFIC_TEST=""
if [ $# -eq 1 ]; then
    SPECIFIC_TEST="$1"
elif [ $# -gt 1 ]; then
    echo -e "${RED}❌ Too many arguments${NC}"
    show_usage
    exit 1
fi

# Create results directory
mkdir -p "$RESULTS_DIR"

# Clean up results directory for fresh start
if [ -d "$RESULTS_DIR" ] && [ "$(ls -A "$RESULTS_DIR" 2>/dev/null)" ]; then
    echo "🧹 Cleaning up previous results..."
    rm -rf "$RESULTS_DIR"/*
    echo "✅ Results directory cleaned"
fi

# Check if server is running
echo "🔍 Checking if AutoCompChem server is running..."
if ! curl -s "$BASE_URL/actuator/health" > /dev/null; then
    echo -e "${RED}❌ Server not responding at $BASE_URL${NC}"
    echo "Please start the AutoCompChem server first"
    exit 1
fi

echo -e "${GREEN}✅ Server is running${NC}"
echo ""

# Check if test directory exists
if [ ! -d "$TEST_DIR" ]; then
    echo -e "${RED}❌ Test directory '$TEST_DIR' not found${NC}"
    exit 1
fi

# Check if jq is available
if ! command -v jq &> /dev/null; then
    echo -e "${RED}❌ jq is required but not installed${NC}"
    echo "Install it with: brew install jq"
    exit 1
fi

# Initialize counters
total_tests=0
successful_tests=0
failed_tests=0

if [ -n "$SPECIFIC_TEST" ]; then
    # Run specific test
    json_file="$TEST_DIR/${SPECIFIC_TEST}${JSONSUFFIX}"
    
    if [ ! -f "$json_file" ]; then
        echo -e "${RED}❌ Test file not found: $json_file${NC}"
        echo ""
        echo "Available tests:"
        for file in "$TEST_DIR"/*"$JSONSUFFIX"; do
            if [ -f "$file" ]; then
                test_name=$(basename "$file" "$JSONSUFFIX")
                echo "  - $test_name"
            fi
        done
        exit 1
    fi
    
    echo -e "${BLUE}🧪 Running Single Test: $SPECIFIC_TEST${NC}"
    echo "📁 Test file: $json_file"
    echo "🌐 Server URL: $BASE_URL"
    echo ""
    
    if run_single_test "$json_file" "$SPECIFIC_TEST"; then
        echo -e "${GREEN}🎉 Test passed!${NC}"
        exit 0
    else
        echo -e "${RED}❌ Test failed!${NC}"
        exit 1
    fi
else
    # Run all tests
    echo -e "${BLUE}🧪 Starting AutoCompChem REST API Tests (All Tests)${NC}"
    echo "📁 Test directory: $TEST_DIR"
    echo "🌐 Server URL: $BASE_URL"
    echo ""
    
    # Check if any test files exist
    test_files=("$TEST_DIR"/*"$JSONSUFFIX")
    if [ ! -f "${test_files[0]}" ]; then
        echo -e "${YELLOW}⚠️  No JSON test files found in $TEST_DIR${NC}"
        exit 1
    fi
    
    # Loop through all JSON files in the test directory
    for json_file in "$TEST_DIR"/*"$JSONSUFFIX"; do
        if [ ! -f "$json_file" ]; then
            continue
        fi
        
        # Extract filename without path and extension
        test_name=$(basename "$json_file" "$JSONSUFFIX")
        
        if run_single_test "$json_file" "$test_name"; then
            ((successful_tests++))
        else
            ((failed_tests++))
        fi
        
        ((total_tests++))
        echo ""
        
        # Small delay between requests to avoid overwhelming the server
        sleep 0.5
    done
    
    # Print summary
    echo -e "${BLUE}📊 Test Summary${NC}"
    echo "=================="
    echo "Total tests: $total_tests"
    echo -e "Successful: ${GREEN}$successful_tests${NC}"
    echo -e "Failed: ${RED}$failed_tests${NC}"
    
    if [ $failed_tests -eq 0 ]; then
        echo -e "${GREEN}🎉 All tests passed!${NC}"
        exit 0
    else
        echo -e "${YELLOW}⚠️  Some tests failed. Check the results in $RESULTS_DIR${NC}"
        exit 1
    fi
fi 
