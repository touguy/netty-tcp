https://gemini.google.com/share/69e07b09859c

1단계: 서버 실행
TcpApplication을 실행합니다. 콘솔에 === Netty TCP Server Started on port: 8888 === 로그가 나오면 성공입니다.

2단계: 기본 접속 테스트 (Telnet 사용)
터미널을 열고 접속합니다.
~~~Bash
telnet localhost 8888
# 결과: "WELCOME to TCP Gateway" 메시지 수신
~~~

아무 글자나 입력하면 "ECHO: [입력값]"이 돌아옵니다.

3단계: Over-session (최대 세션 초과) 테스트
기본 설정상 maxSessions는 5입니다.

터미널 창을 5개 열어서 모두 접속합니다.

6번째 터미널에서 접속을 시도합니다.

결과: "ERR: Server Busy" 메시지를 받고 즉시 연결이 끊어집니다. (rejectNewIfFull = true 기본값)

4단계: Hot Deploy 테스트 (설정 변경)
서버를 끄지 않은 상태에서 REST API를 호출해 정책을 바꿉니다.

요청 (Reject 정책 -> Kick Old 정책으로 변경, Max Session을 2로 축소)

~~~Bash
curl -X POST http://localhost:8080/api/admin/config \
     -H "Content-Type: application/json" \
     -d '{"maxSessions": 2, "rejectNewIfFull": false}'
~~~
     
5단계: 변경된 정책 확인
현재 연결된 세션이 있다면 일단 모두 끊고 다시 테스트해 봅니다.

터미널 1 접속 -> 성공

터미널 2 접속 -> 성공 (현재 세션 2/2)

터미널 3 접속 -> 성공!

확인: 먼저 접속했던 터미널 1의 연결이 서버에 의해 강제로 끊어졌는지 확인합니다. (SessionManager 로그: Kicking oldest session...)

6단계: Polling / KeepAlive 테스트
readerIdleTime은 기본 30초입니다.

접속 후 30초간 아무것도 입력하지 않고 기다립니다.

결과: 서버로부터 PING 메시지가 날아오는지 확인합니다. (로그: Reader Idle... Sending KeepAlive Probe...)