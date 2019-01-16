<img src="https://github.com/mangotomato/uploads/blob/master/Guardian_logo.png" alt="Guardian Logo" width="50%">

# Guardian: Guardian of your APIs (http/https)

## Introduction

We have the requirement for publishing our service to public more urgent as the rapid bussiness explosion. So, Guardian comes up.
As a API gateway, guardian is a edge service, provide single entry to access backend service. Guardian designed for protecting backend service.

Guardian has the following features:
- **API Lifecycle Management**
- **Dynamic Routing**
- **Dynamic Filters**
- **Dynamic Configuration**
- **Traffic Control**
- **Parameter Check**
- **Data Transform**
- **Botblocker**
- **Authentication**
- **Real-time Monitor**

## Documentation

See the [中文文档](https://github.com/mangotomato/guardian/wiki/Guardian%E4%BB%8B%E7%BB%8D) for Chinese document.

## Overall structure

<img src="https://github.com/mangotomato/uploads/blob/master/architecturev1.0.png" alt="Overall structure">

## Design

Guardian Adapted <a href="https://github.com/Netflix/zuul/wiki/How-it-Works">zuul</a>'s fiter design, below image shows zuul request lifecycle, filter is everywhere.
<img src="https://github.com/mangotomato/uploads/blob/master/zuul_request_lifecycle.png" width="75%" height="75%" alt="zuul filter lifecycle">

Guardin introduced the Servlet 3.0 asynchronous feature，which can hold more requests.
<img src="https://github.com/mangotomato/uploads/blob/master/async_servlet.png" alt="async servlet">
