# MongoDB 在 Spring 项目中的两种常见使用方式：CRUD 示例

下面用一个通用的 `User` 用户文档，分别演示：

- `MongoTemplate` 写法：手动构造 `Query`、`Criteria`、`Update`
- `MongoRepository` 接口式写法：继承 `MongoRepository`，直接调用现成方法或按方法名生成查询

示例包名统一使用 `com.example.demo`，实际项目中替换成自己的包名即可。

## 一、公共准备

### 1. Maven 依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>
```

### 2. MongoDB 配置

文件：`src/main/resources/application.yml`

```yaml
spring:
  data:
    mongodb:
      host: localhost
      port: 27017
      database: demo

logging:
  level:
    org.springframework.data.mongodb.core: debug
```

有了上面的依赖和配置后，Spring Boot 会自动创建 `MongoTemplate`，业务代码里直接注入即可。

### 3. 启动类

文件：`src/main/java/com/example/demo/DemoApplication.java`

```java
package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

### 4. 文档对象

文件：`src/main/java/com/example/demo/domain/User.java`

```java
package com.example.demo.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "users")
public class User {
    @Id
    private String id;

    @Indexed
    private String username;

    @Indexed(unique = true)
    private String email;

    private Integer age;
    private Date createTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
```

## 二、MongoTemplate 写法

### 1. MongoTemplate 是如何创建的

Spring Boot 推荐做法是自动创建：

```text
application.yml
    -> spring.data.mongodb.host / port / database
    -> Spring Boot 自动配置 MongoClient
    -> Spring Boot 自动配置 MongoTemplate
    -> Service 中直接注入 MongoTemplate
```

业务代码中这样用：

```java
@Autowired
private MongoTemplate mongoTemplate;
```

如果你不是用 Spring Boot 自动配置，也可以手动创建一个 `MongoTemplate`。

文件：`src/main/java/com/example/demo/config/MongoTemplateConfig.java`

```java
package com.example.demo.config;

import com.mongodb.MongoClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class MongoTemplateConfig {
    @Value("${spring.data.mongodb.host}")
    private String host;

    @Value("${spring.data.mongodb.port}")
    private int port;

    @Value("${spring.data.mongodb.database}")
    private String database;

    @Bean
    public MongoClient mongoClient() {
        return new MongoClient(host, port);
    }

    @Bean
    public MongoTemplate mongoTemplate(MongoClient mongoClient) {
        return new MongoTemplate(mongoClient, database);
    }
}
```

一般情况下，使用 Spring Boot 时不需要写这个配置类。

### 2. Service 接口

文件：`src/main/java/com/example/demo/service/UserTemplateService.java`

```java
package com.example.demo.service;

import com.example.demo.domain.User;

import java.util.List;

public interface UserTemplateService {
    User create(User user);

    User getById(String id);

    List<User> listByUsername(String username);

    User updateAge(String id, Integer age);

    void deleteById(String id);
}
```

### 3. Service 实现类

文件：`src/main/java/com/example/demo/service/impl/UserTemplateServiceImpl.java`

```java
package com.example.demo.service.impl;

import com.example.demo.domain.User;
import com.example.demo.service.UserTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class UserTemplateServiceImpl implements UserTemplateService {
    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public User create(User user) {
        user.setCreateTime(new Date());
        return mongoTemplate.insert(user);
    }

    @Override
    public User getById(String id) {
        return mongoTemplate.findById(id, User.class);
    }

    @Override
    public List<User> listByUsername(String username) {
        Query query = new Query();
        query.addCriteria(Criteria.where("username").regex(username));
        return mongoTemplate.find(query, User.class);
    }

    @Override
    public User updateAge(String id, Integer age) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));

        Update update = new Update();
        update.set("age", age);

        return mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                User.class);
    }

    @Override
    public void deleteById(String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        mongoTemplate.remove(query, User.class);
    }
}
```

### 4. Controller

文件：`src/main/java/com/example/demo/controller/UserTemplateController.java`

```java
package com.example.demo.controller;

import com.example.demo.domain.User;
import com.example.demo.service.UserTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/template/users")
public class UserTemplateController {
    @Autowired
    private UserTemplateService userTemplateService;

    @PostMapping
    public User create(@RequestBody User user) {
        return userTemplateService.create(user);
    }

    @GetMapping("/{id}")
    public User getById(@PathVariable String id) {
        return userTemplateService.getById(id);
    }

    @GetMapping
    public List<User> listByUsername(@RequestParam String username) {
        return userTemplateService.listByUsername(username);
    }

    @PutMapping("/{id}/age")
    public User updateAge(@PathVariable String id, @RequestParam Integer age) {
        return userTemplateService.updateAge(id, age);
    }

    @DeleteMapping("/{id}")
    public void deleteById(@PathVariable String id) {
        userTemplateService.deleteById(id);
    }
}
```

### 5. 请求示例

新增：

```http
POST /template/users
Content-Type: application/json

{
  "username": "zhangsan",
  "email": "zhangsan@example.com",
  "age": 20
}
```

根据 ID 查询：

```http
GET /template/users/用户ID
```

按用户名模糊查询：

```http
GET /template/users?username=zhang
```

修改年龄：

```http
PUT /template/users/用户ID/age?age=21
```

删除：

```http
DELETE /template/users/用户ID
```

## 三、MongoRepository 接口式写法

### 1. Repository 接口配置

如果 Repository 接口放在启动类包名的子包下，比如：

```text
com.example.demo.DemoApplication
com.example.demo.repository.UserRepository
```

Spring Boot 会自动扫描，一般不需要额外配置。

如果 Repository 不在启动类包名的子包下，可以显式配置：

文件：`src/main/java/com/example/demo/config/MongoRepositoryConfig.java`

```java
package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "com.example.demo.repository")
public class MongoRepositoryConfig {
}
```

### 2. Repository 接口

文件：`src/main/java/com/example/demo/repository/UserRepository.java`

```java
package com.example.demo.repository;

import com.example.demo.domain.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);

    List<User> findByUsernameContaining(String username);

    void deleteByEmail(String email);
}
```

说明：

- `MongoRepository<User, String>` 中的 `User` 是文档类，`String` 是 `@Id` 字段类型。
- `save`、`findById`、`findAll`、`deleteById` 等 CRUD 方法不用自己写。
- `findByEmail`、`findByUsernameContaining` 会根据方法名自动生成查询。

### 3. Service 接口

文件：`src/main/java/com/example/demo/service/UserRepositoryService.java`

```java
package com.example.demo.service;

import com.example.demo.domain.User;

import java.util.List;

public interface UserRepositoryService {
    User create(User user);

    User getById(String id);

    List<User> listByUsername(String username);

    User updateAge(String id, Integer age);

    void deleteById(String id);
}
```

### 4. Service 实现类

文件：`src/main/java/com/example/demo/service/impl/UserRepositoryServiceImpl.java`

```java
package com.example.demo.service.impl;

import com.example.demo.domain.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.UserRepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class UserRepositoryServiceImpl implements UserRepositoryService {
    @Autowired
    private UserRepository userRepository;

    @Override
    public User create(User user) {
        user.setCreateTime(new Date());
        return userRepository.insert(user);
    }

    @Override
    public User getById(String id) {
        return userRepository.findById(id).orElse(null);
    }

    @Override
    public List<User> listByUsername(String username) {
        return userRepository.findByUsernameContaining(username);
    }

    @Override
    public User updateAge(String id, Integer age) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return null;
        }
        user.setAge(age);
        return userRepository.save(user);
    }

    @Override
    public void deleteById(String id) {
        userRepository.deleteById(id);
    }
}
```

补充：

- `insert(user)`：只做新增，`_id` 已存在会报错。
- `save(user)`：`_id` 不存在时新增，`_id` 存在时更新。
- 所以新增更推荐用 `insert`，修改更推荐先查出来再 `save`。

### 5. Controller

文件：`src/main/java/com/example/demo/controller/UserRepositoryController.java`

```java
package com.example.demo.controller;

import com.example.demo.domain.User;
import com.example.demo.service.UserRepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/repository/users")
public class UserRepositoryController {
    @Autowired
    private UserRepositoryService userRepositoryService;

    @PostMapping
    public User create(@RequestBody User user) {
        return userRepositoryService.create(user);
    }

    @GetMapping("/{id}")
    public User getById(@PathVariable String id) {
        return userRepositoryService.getById(id);
    }

    @GetMapping
    public List<User> listByUsername(@RequestParam String username) {
        return userRepositoryService.listByUsername(username);
    }

    @PutMapping("/{id}/age")
    public User updateAge(@PathVariable String id, @RequestParam Integer age) {
        return userRepositoryService.updateAge(id, age);
    }

    @DeleteMapping("/{id}")
    public void deleteById(@PathVariable String id) {
        userRepositoryService.deleteById(id);
    }
}
```

### 6. 请求示例

新增：

```http
POST /repository/users
Content-Type: application/json

{
  "username": "lisi",
  "email": "lisi@example.com",
  "age": 22
}
```

根据 ID 查询：

```http
GET /repository/users/用户ID
```

按用户名模糊查询：

```http
GET /repository/users?username=li
```

修改年龄：

```http
PUT /repository/users/用户ID/age?age=23
```

删除：

```http
DELETE /repository/users/用户ID
```

## 四、两种方式对比

| 对比项 | MongoTemplate | MongoRepository |
| --- | --- | --- |
| 新增 | `mongoTemplate.insert(user)` | `userRepository.insert(user)` 或 `save(user)` |
| 查询 | `Query` + `Criteria` | `findById`、`findByEmail`、`findByUsernameContaining` |
| 修改 | `Update` + `findAndModify` | 查出来改字段，再 `save` |
| 删除 | `mongoTemplate.remove(query, User.class)` | `deleteById`、`deleteByEmail` |
| 适用场景 | 复杂查询、动态条件、复杂更新 | 简单 CRUD、标准查询 |

简单 CRUD 优先用 Repository，复杂查询或复杂更新再用 MongoTemplate。
