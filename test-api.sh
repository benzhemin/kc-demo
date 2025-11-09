#!/bin/bash

# JWT Validation Demo - Test Script
# This script tests all the API endpoints

echo "========================================="
echo "JWT Validation Demo - Testing Script"
echo "========================================="
echo ""

BASE_URL="http://localhost:8080/api"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print section headers
print_header() {
    echo ""
    echo -e "${YELLOW}=========================================${NC}"
    echo -e "${YELLOW}$1${NC}"
    echo -e "${YELLOW}=========================================${NC}"
    echo ""
}

# Function to test endpoint
test_endpoint() {
    local method=$1
    local endpoint=$2
    local headers=$3
    local description=$4

    echo -e "${GREEN}Testing: $description${NC}"
    echo "Endpoint: $method $endpoint"
    echo ""

    if [ -z "$headers" ]; then
        response=$(curl -s -X $method "$BASE_URL$endpoint")
    else
        response=$(curl -s -X $method "$BASE_URL$endpoint" -H "$headers")
    fi

    echo "Response:"
    echo "$response" | jq '.' 2>/dev/null || echo "$response"
    echo ""
}

# 1. Test public endpoints
print_header "1. Testing Public Endpoints (No Auth Required)"

test_endpoint "GET" "/public/hello" "" "Public Hello"
test_endpoint "GET" "/public/health" "" "Health Check"
test_endpoint "GET" "/public/info" "" "API Info"

# 2. Generate tokens
print_header "2. Generating Test Tokens"

echo -e "${GREEN}Generating USER token...${NC}"
USER_TOKEN_RESPONSE=$(curl -s "$BASE_URL/public/mock/user-token")
USER_TOKEN=$(echo $USER_TOKEN_RESPONSE | jq -r '.token')
echo "USER Token generated (first 50 chars): ${USER_TOKEN:0:50}..."
echo ""

echo -e "${GREEN}Generating ADMIN token...${NC}"
ADMIN_TOKEN_RESPONSE=$(curl -s "$BASE_URL/public/mock/admin-token")
ADMIN_TOKEN=$(echo $ADMIN_TOKEN_RESPONSE | jq -r '.token')
echo "ADMIN Token generated (first 50 chars): ${ADMIN_TOKEN:0:50}..."
echo ""

# 3. Test user endpoints
print_header "3. Testing User Endpoints (USER token)"

test_endpoint "GET" "/user/profile" "Authorization: Bearer $USER_TOKEN" "User Profile"
test_endpoint "GET" "/user/token-info" "Authorization: Bearer $USER_TOKEN" "Token Info"
test_endpoint "GET" "/user/hello" "Authorization: Bearer $USER_TOKEN" "User Hello"

# 4. Test user trying to access admin endpoint (should fail)
print_header "4. Testing Authorization (USER trying ADMIN endpoint - should FAIL)"

echo -e "${RED}Expected: 403 Forbidden${NC}"
test_endpoint "GET" "/admin/dashboard" "Authorization: Bearer $USER_TOKEN" "Admin Dashboard (USER token)"

# 5. Test admin endpoints
print_header "5. Testing Admin Endpoints (ADMIN token)"

test_endpoint "GET" "/admin/dashboard" "Authorization: Bearer $ADMIN_TOKEN" "Admin Dashboard"
test_endpoint "GET" "/admin/info" "Authorization: Bearer $ADMIN_TOKEN" "Admin Info"

# 6. Test without token (should fail)
print_header "6. Testing Without Token (should FAIL)"

echo -e "${RED}Expected: 401 Unauthorized${NC}"
test_endpoint "GET" "/user/profile" "" "User Profile (no token)"

# Summary
print_header "Testing Complete!"

echo -e "${GREEN}✓ All tests executed${NC}"
echo ""
echo "Generated Tokens:"
echo "  USER_TOKEN:  $USER_TOKEN"
echo "  ADMIN_TOKEN: $ADMIN_TOKEN"
echo ""
echo "You can use these tokens to test manually:"
echo "  curl http://localhost:8080/api/user/profile -H \"Authorization: Bearer \$USER_TOKEN\""
echo ""
