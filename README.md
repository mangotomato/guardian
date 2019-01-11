<img src="https://github.com/mangotomato/uploads/blob/master/Guardian%20logo.png" alt="Guardian Logo" width="50%">

# Guardian: Guardian of your APIs (http/https)

## Introduction

We have the requirement for publishing our service to public more urgent as the rapid bussiness explosion. So, Guardian comes up.
As a api gateway, guardian is a edge service, provide single entry to access backend service. Guardian designed for protecting backend service, to guarantee service reliability.

Guardian has the following features:
- **Api management**:  Guardian provide the web console to management api's lifecycle. You can define, release and downline a api.
- **Dynamic filters**:  Guardian supports dynamic load filters. For example, you can upload a new filter's source code, guardian able to poll the change, compile the source code, and plugin the filter to current filter's pipeline. Basicly, you can active/inactive a filter.
- **Dynamic configuration**: Guardian integrate with ctrip's Apollo, you can easily change Guardian's state. For example, httpclient's Socket timeout, database confs etc.
- **Ip access control**: Guardian integrate with alibaba's Sentinel, Sentinel provide the 'black and white ip list' control.
- **Flow control**: Guardian integrate with alibaba's Sentinel, provide flow control.
- **Rewrite**: Guardian will buffer request stream, So, you can checking, filter, transform the request body by defining rewrite rules.
- **Botblocker**: Guardian provide a basic anti-reptile filter.
- **Authentication**: Guardian supports AppKey and OpenID-Connect authetication.
- **Real-time monitor**: Guardian integrate with meituan's Cat, you can real-time monitor Guardian's state. Also, Cat provide alarm support.

## Documentation

See the [中文文档](https://github.com/mangotomato/guardian/wiki/Guardian%E4%BB%8B%E7%BB%8D) for Chinese document.

## Overall structure
<img src="https://github.com/mangotomato/uploads/blob/master/architecturev1.0.png" alt="整体架构">
