<img src="https://github.com/mangotomato/uploads/blob/master/Guardian_logo.png" alt="Guardian Logo" width="50%">

# Guardian: Guardian of your APIs (http/https)

## Introduction

We have the requirement for publishing our service to public more urgent as the rapid bussiness explosion. So, Guardian comes up.
As a API gateway, guardian is a edge service, provide single entry to access backend service. Guardian designed for protecting backend service.

Guardian has the following features:
- **API Lifecycle Management**  

1. Support API define, release, downline.  
2. Support API's version management, version rollback.  

- **Dynamic Routing**  

1. Support ant-style URL mapping, dynamic routing to origin server.    
2. Support upstream healthcheck.      

- **Dynamic Filters**  

&nbsp;&nbsp;&nbsp;&nbsp;Support dynamic loading filter, no need to restart application.  

- **Traffic Control**  

1. Flow control can be used to control the visited frequency of the API, the request frequency of the APP, and the request frequency of the user.  
2. The time unit of flow control can be minutes, hours, and days.

- **Request verification**  

1. Support parameter type, parameter value (range, enumeration, regular) check, invalid check will be directly rejected, to reduce the waste of resources caused by invalid requests to the backend.  

- **Data Transform**  

&nbsp;&nbsp;&nbsp;&nbsp;Pre- and post-end data translation is implemented by configuring mapping rules.  
- Support for data conversion of front-end requests.  
- Support for data conversion that returns results. 

- **Botblocker**  

&nbsp;&nbsp;&nbsp;&nbsp;Provider a Basic anti-replite filter.  

- **Safety**

1. Support AppKey, OpenID-Connect authentication, support HMAC (SHA-1, SHA-256) algorithm for signature.  
2. Anti-attack, anti-injection, request anti-replay, request anti-tampering.  

- **Real-time Monitor**  

1. Provide visual API real-time monitoring.  
2. Configurable warning mode (SMS, Email), subscribe to warning information.  

## Documentation

See the [中文文档](https://github.com/mangotomato/guardian/wiki/Guardian%E4%BB%8B%E7%BB%8D) for Chinese document.

## Overall structure

<img src="https://github.com/mangotomato/uploads/blob/master/architecturev1.0.png" alt="Overall structure">

## Design

Guardian Adapted <a href="https://github.com/Netflix/zuul/wiki/How-it-Works">zuul</a>'s fiter design, below image shows zuul request lifecycle, filter is everywhere.
<img src="https://github.com/mangotomato/uploads/blob/master/zuul_request_lifecycle.png" width="75%" height="75%" alt="zuul filter lifecycle">

Guardin introduced the Servlet 3.0 asynchronous feature，which can hold more requests.
<img src="https://github.com/mangotomato/uploads/blob/master/async_servlet.png" alt="async servlet">
