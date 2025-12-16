#!/bin/bash

# 1. Kill any existing processes on ports
fuser -k 8080/tcp
fuser -k 8888/tcp

# 2. Start the Spring Boot application in the background
echo "Starting Spring Boot App..."
./gradlew bootRun > server.log 2>&1 &
SERVER_PID=$!

# Wait for server to start (polling)
echo "Waiting for server to launch on port 8080..."
for i in {1..60}; do
    if nc -z localhost 8080; then
        echo "Server is UP!"
        break
    fi
    sleep 2
done

# 3. Connect a TCP Client (nc)
echo "Connecting TCP Client..."
nc localhost 8888 > client_output.txt &
CLIENT_PID=$!
sleep 2 # wait for connection

# 4. Send Broadcast Request
echo "Sending Broadcast Request via REST API..."
curl -X POST -H "Content-Type: text/plain" -d "Hello Antigravity!" http://localhost:8080/api/message/broadcast

sleep 2

# 5. Check Output
echo "Checking client output..."
if grep -q "Hello Antigravity!" client_output.txt; then
    echo "SUCCESS: Message received by TCP client."
else
    echo "FAILURE: Message NOT found in client output."
    cat client_output.txt
fi

# 6. Cleanup
kill $SERVER_PID
kill $CLIENT_PID
fuser -k 8080/tcp
fuser -k 8888/tcp
