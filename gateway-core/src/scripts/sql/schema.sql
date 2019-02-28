# ************************************************************
# Sequel Pro SQL dump
# Version 4541
#
# http://www.sequelpro.com/
# https://github.com/sequelpro/sequelpro
#
# Host: 192.168.0.85 (MySQL 5.6.35-log)
# Database: gateway
# Generation Time: 2019-02-28 09:40:27 +0000
# ************************************************************


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


# Dump of table api
# ------------------------------------------------------------

CREATE TABLE `api` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `path` varchar(60) NOT NULL DEFAULT '' COMMENT 'API PATH',
  `name` varchar(120) DEFAULT NULL COMMENT 'API 名称',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='API';



# Dump of table api_flow_rule
# ------------------------------------------------------------

CREATE TABLE `api_flow_rule` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `api_id` int(11) NOT NULL COMMENT 'API外键',
  `flow_rule_id` int(11) NOT NULL COMMENT '流控规则ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='API流控规则';



# Dump of table app
# ------------------------------------------------------------

CREATE TABLE `app` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `app_id` varchar(32) NOT NULL DEFAULT '' COMMENT '应用ID',
  `client_id` varchar(32) NOT NULL DEFAULT '' COMMENT '客户ID',
  `name` varchar(60) NOT NULL DEFAULT '' COMMENT '应用名称',
  `app_key` varchar(60) NOT NULL DEFAULT '' COMMENT '应用KEY',
  `app_secret` varchar(60) NOT NULL DEFAULT '' COMMENT '应用SECRET',
  `remark` varchar(120) NOT NULL DEFAULT '' COMMENT '应用描述',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '伪删除标记',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_user` varchar(60) DEFAULT '' COMMENT '创建人',
  `modify_time` datetime DEFAULT NULL COMMENT '修改时间',
  `modify_user` varchar(60) DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='应用';



# Dump of table flow_rule
# ------------------------------------------------------------

CREATE TABLE `flow_rule` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(60) NOT NULL DEFAULT '' COMMENT '流控名称',
  `limit` int(11) NOT NULL DEFAULT '-1' COMMENT 'API流控',
  `limit_client` int(11) NOT NULL DEFAULT '-1' COMMENT '用户流控',
  `limit_app` int(11) NOT NULL DEFAULT '-1' COMMENT '应用流控',
  `time_unit` varchar(20) NOT NULL DEFAULT 'SECOND' COMMENT '流控单位',
  `remark` varchar(120) NOT NULL DEFAULT '“”' COMMENT '备注',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='流控规则';



# Dump of table gateway_filter
# ------------------------------------------------------------

CREATE TABLE `gateway_filter` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `filter_id` varchar(45) DEFAULT NULL COMMENT '过滤器id',
  `revision` int(11) DEFAULT NULL COMMENT '版本',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `is_active` tinyint(1) DEFAULT '0' COMMENT '是否是活跃',
  `is_canary` tinyint(1) DEFAULT '0' COMMENT '是否是灰度',
  `filter_code` longtext COMMENT 'filter代码',
  `filter_type` varchar(45) DEFAULT NULL COMMENT 'filter类型',
  `filter_name` varchar(45) DEFAULT NULL COMMENT '名称',
  `disable_property_name` varchar(45) DEFAULT NULL COMMENT '禁用属性',
  `filter_order` varchar(45) DEFAULT NULL COMMENT '顺序',
  `application_name` varchar(45) DEFAULT NULL COMMENT '应用名称',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='网关filter';




/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
