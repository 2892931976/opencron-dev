namespace java org.opencron.common.job

enum Action {
    PING,
    PATH,
    MONITOR,
    EXECUTE,
    PASSWORD,
    KILL,
    PROXY,
    GUID,
    RESTART
}

struct Request {
     1:string hostName,
     2:i32 port,
     3:Action action,
     4:string password,
     5:map<string, string> params
}

struct Response {
     1:i64 recordId,
     2:Action action,
     3:map<string, string> result,
     4:i32 exitCode,
     5:bool success,
     6:i64 startTime,
     7:i64 endTime,
     8:string message
}

service Opencron {
 Response ping(1:Request request),
 Response path(1:Request request),
 Response monitor(1:Request request)
 Response execute(1:Request request),
 Response password(1:Request request),
 Response kill(1:Request request),
 Response proxy(1:Request request),
 Response guid(1:Request request),
 void restart(1:Request request)
}