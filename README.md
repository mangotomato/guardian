<img src="https://github.com/mangotomato/uploads/blob/master/Guardian%20logo.png" alt="Guardian Logo" width="50%">

# Guardian: Guardian of your APIs (http/https)
## Introduction
We have the requirement to publish our service to public more urgent as the rapid bussiness explosion. So, Guardian comes up.
As a api gateway, guardian is a edge service, provide single entry to access backend services. Guardian designed for protecting backend service, to guarantee service reliability.

Guardian has the following features:
- **Api management**:  guardian provide the web console to management api's lifecycle. You can define,release and downline a api.
- **Dynamic filters**:  dynamic load filters. for example, You can upload a new filter's source code, guardian able to poll the change, compile the source coude, and plugin the filter to current filter's pipeline. basicly, You can active/inactive a filter.
- **Dynamic configuration**: guardian integrate with ctrip's Apollo, You can easily change Guardian's state. for example, httpclient's Socket timeout, database configs etc.
- **Ip access control**: guardian integrate with alibaba's Sentinel, Sentinel privide the Black and white ip list control.
- **Rlow control**: guardian integrate with alibaba's Sentinel, Sentinel privide flow control.
- **Rewrite**: guardian will buffer request stream, So, you can checking、transform the request body by defing rewrite rules.
- **Botblocker**: guardian provide a basic Anti-reptile filter.
- **Authentication**: guardian support AppKey and OpenID-Connect authetication.
- **Real-time monitoring**: guardian integrate with meituan's Cat, Your can real-time monitor guardian's state. also, cat provide alarm.

## Overall structure
<img src="https://github.com/mangotomato/uploads/blob/master/architecture.png" alt="整体架构">
