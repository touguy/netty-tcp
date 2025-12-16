#!/bin/bash

# 1. Cleanup
fuser -k 8080/tcp
fuser -k 8888/tcp

# 2. Start Server
echo "Starting Spring Boot App..."
gradle bootRun > server_verify_specific.log 2>&1 &
SERVER_PID=$!

# Wait for server
echo "Waiting for server to launch on port 8080..."
for i in {1..60}; do
    if nc -z localhost 8080; then
        echo "Server is UP!"
        break
    fi
    sleep 2
done

# 3. Connect TCP Client
echo "Connecting TCP Client..."
nc localhost 8888 > client_specific.txt &
CLIENT_PID=$!
sleep 2

# 4. Get Client ID
echo "Fetching Client ID..."
CLIENT_ID=$(curl -s http://localhost:8080/api/message/clients | grep -o '"[^"]*"' | sed 's/"//g' | head -n 1)
echo "Detected Client ID: $CLIENT_ID"

if [ -z "$CLIENT_ID" ]; then
    echo "FAILURE: Could not retrieve Client ID"
    kill $SERVER_PID
    kill $CLIENT_PID
    exit 1
fi

# 5. Send Message to Specific Client
echo "Sending Message to $CLIENT_ID..."
curl -X POST -H "Content-Type: text/plain" -d "Secret Message for You" "http://localhost:8080/api/message/send/$CLIENT_ID"

sleep 2

# 6. Verify
echo "Checking client output..."
if grep -q "Secret Message for You" client_specific.txt; then
    echo "SUCCESS: Specific message received."
else
    echo "FAILURE: Message NOT found."
    cat client_specific.txt
fi

# 7. Cleanup
kill $SERVER_PID
kill $CLIENT_PID
fuser -k 8080/tcp
fuser -k 8888/tcp
