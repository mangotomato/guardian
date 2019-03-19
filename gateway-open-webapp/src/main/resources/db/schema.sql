CREATE TABLE `api` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `path` varchar(60) NOT NULL DEFAULT '' COMMENT 'API PATH',
  `name` varchar(120) DEFAULT NULL COMMENT 'API 名称',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='API';

CREATE TABLE `api_flow_rule` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `api_id` int(11) NOT NULL COMMENT 'API外键',
  `flow_rule_id` int(11) NOT NULL COMMENT '流控规则ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='API流控规则';

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
