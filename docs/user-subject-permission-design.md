# 用户主体与权限设计梳理

## 1. 当前推荐模型

当前项目更适合采用三层模型：

- `sys_user`：登录凭证，只表示“谁可以登录”。
- `sys_user_subject`：账号主体，只表示“这个账号当前属于平台、租户还是商户”。
- `sys_role`、`sys_role_menu`、`sys_role_data_scope`：权限模型，只表示“这个主体账号拥有哪些菜单、按钮和数据范围”。

不要把“负责人、财务、运营、开发者、管理员”等身份再放到 `sys_user_subject`。这些都属于角色和权限，应该由 `sys_role` 表达。

## 2. 登录凭证

`sys_user` 是统一登录账号表，保存用户名、密码哈希、手机号、邮箱、状态、基础资料和验证器信息。

它不应该保存：

- `tenant_id`
- `merchant_id`
- `dept_id`
- `role_id`
- `super_admin`

这些字段不是登录凭证本身，而是账号登录后进入的业务主体和权限上下文。

登录流程建议：

1. 用户用 `username/password` 登录。
2. 系统查询 `sys_user`。
3. 系统通过 `sys_user_subject.user_id` 找到这个账号唯一主体。
4. 系统通过 `sys_user_subject.role_id` 找到角色。
5. JWT 写入 `userId`、`subjectType`、`tenantId`、`merchantId`、`deptId`、`roleId`。
6. 后续接口从 JWT 还原请求上下文。

## 3. 主体模型

`sys_user_subject` 当前只保留必要字段：

- `user_id`：登录账号 ID。
- `subject_type`：主体类型，取值 `PLATFORM`、`TENANT`、`MERCHANT`。
- `tenant_id`：租户 ID。租户主体和商户主体必填。
- `merchant_id`：商户 ID。商户主体必填。
- `dept_id`：部门 ID。后台部门数据权限需要时使用。
- `role_id`：角色 ID。
- `status`：主体关系是否启用。

主体字段含义：

- 平台主体：`subject_type = PLATFORM`，`tenant_id = null`，`merchant_id = null`。
- 租户主体：`subject_type = TENANT`，`tenant_id != null`，`merchant_id = null`。
- 商户主体：`subject_type = MERCHANT`，`tenant_id != null`，`merchant_id != null`。

当前你设计的是“一个账号只能有一个主体”，所以 `sys_user_subject.user_id` 唯一是合理的。后续如果要支持一个人同时管理多个租户或多个商户，再改为多主体关系，并在登录后增加主体切换。

## 4. 平台主体

你的理解基本正确：平台主体就是系统管理人员。

平台主体可以做的事情：

- 管理平台级角色。
- 管理平台级部门。
- 创建租户。
- 创建或绑定租户主体账号。
- 查看平台级运营和管理数据。

需要纠正的一点是：“平台可以添加角色和部门，但是下级看不到，因为没有权限”这个说法方向对，但实现上不应只依赖菜单权限，还要依赖数据归属。

建议规则：

- 平台角色：`sys_role.role_scope = PLATFORM`，`tenant_id = null`。
- 平台部门：只归平台使用，不给租户和商户查询。
- 租户登录后只能看到 `role_scope = TENANT` 且 `tenant_id = 当前租户` 的角色。
- 商户登录后只能看到 `role_scope = MERCHANT` 且 `tenant_id = 当前租户` 的角色。

平台超级管理员不要放在 `sys_user_subject` 中单独标记，建议用角色表达：

- `subject_type = PLATFORM`
- `sys_role.auth = SUPER_ADMIN`

## 5. 租户主体

租户主体是平台创建出来的租户管理人员。

租户主体可以做的事情：

- 管理自己租户下的角色。
- 管理自己租户下的部门。
- 管理自己租户下的用户。
- 创建商户。
- 创建或绑定商户主体账号。
- 给租户用户或商户用户分配角色。

租户主体不能看到：

- 平台主体用户。
- 平台部门。
- 平台角色。
- 其他租户的数据。
- 其他租户下的商户。

建议规则：

- 租户角色：`role_scope = TENANT`，`tenant_id = 当前租户`。
- 租户部门：`tenant_id = 当前租户`。
- 租户用户：`sys_user_subject.subject_type = TENANT` 且 `tenant_id = 当前租户`。
- 租户创建商户用户时，只能绑定自己租户下的商户。

## 6. 商户主体

商户主体是具体商户的管理或操作账号。

商户主体可以做的事情：

- 查看和管理自己商户的数据。
- 根据角色权限查看订单、代付、账务、回调、通知等功能。
- 如果业务需要，可以管理商户内部账号。

商户主体不能看到：

- 平台用户、角色、部门。
- 租户用户、角色、部门。
- 其他商户的数据。

建议规则：

- 商户角色：`role_scope = MERCHANT`，`tenant_id = 当前租户`。
- 商户用户：`subject_type = MERCHANT`，`tenant_id = 当前租户`，`merchant_id = 当前商户`。
- 商户数据查询必须带 `merchant_id = 当前商户`。

如果商户也要自己创建员工账号，建议让商户管理员只能分配 `role_scope = MERCHANT` 的角色，不能分配租户角色或平台角色。

## 7. 角色和权限

角色是权限设计的核心。

`sys_role` 建议保留这些关键字段：

- `role_scope`：角色适用层级，`PLATFORM`、`TENANT`、`MERCHANT`。
- `tenant_id`：角色所属租户。平台角色为空。
- `auth`：角色编码，例如 `SUPER_ADMIN`、`TENANT_ADMIN`、`MERCHANT_ADMIN`。
- `data_scope`：数据范围，例如 `ALL`、`TENANT_ALL`、`SELF_AND_CHILDREN`、`SELF`。

推荐角色示例：

- 平台：`SUPER_ADMIN`、`PLATFORM_ADMIN`、`PLATFORM_OPERATOR`。
- 租户：`TENANT_ADMIN`、`TENANT_FINANCE`、`TENANT_OPERATOR`。
- 商户：`MERCHANT_ADMIN`、`MERCHANT_FINANCE`、`MERCHANT_OPERATOR`。

这些角色名只是默认模板，不需要写死在主体表里。

## 8. 用户创建流程

### 平台创建租户管理员

1. 平台用户创建 `sys_user`。
2. 平台用户创建 `sys_user_subject`。
3. `subject_type = TENANT`。
4. 写入 `tenant_id`。
5. 分配 `role_scope = TENANT` 的角色。

### 租户创建商户管理员

1. 租户用户创建 `sys_user`。
2. 租户用户创建 `sys_user_subject`。
3. `subject_type = MERCHANT`。
4. 写入当前 `tenant_id` 和目标 `merchant_id`。
5. 分配 `role_scope = MERCHANT` 的角色。

### 租户创建租户内部用户

1. 租户用户创建 `sys_user`。
2. 租户用户创建 `sys_user_subject`。
3. `subject_type = TENANT`。
4. 写入当前 `tenant_id` 和可选 `dept_id`。
5. 分配当前租户下的租户角色。

## 9. 权限隔离规则

建议所有后台查询都遵循这组规则：

- 平台主体：可按角色权限访问平台功能。
- 租户主体：所有租户级数据都必须限制 `tenant_id = 当前租户`。
- 商户主体：所有商户级数据都必须限制 `tenant_id = 当前租户` 且 `merchant_id = 当前商户`。
- 查询角色时必须按 `role_scope` 和 `tenant_id` 过滤。
- 查询用户时必须从 `sys_user_subject` 过滤主体范围。
- 分配角色时必须校验角色作用域和当前主体是否匹配。

## 10. 结论

你的整体理解是对的，但需要做三点收敛：

1. `sys_user` 只做登录凭证，不表达平台、租户、商户身份。
2. `sys_user_subject` 只做主体归属，不表达管理员、财务、运营等角色身份。
3. 所有权限身份都放到 `sys_role`，通过 `role_scope`、`auth`、`data_scope` 控制。

这样设计最简单，也最适合当前新项目阶段。后续如果业务复杂，再扩展多主体切换、商户员工体系、租户自定义角色模板即可。
