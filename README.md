## GDUT教务系统模拟登录工具

使用前需要在pom.xml导入如下三个依赖（版本仅供参考，相差不大即可）

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <version>2.2.0.RELEASE</version>
</dependency>
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-lang3</artifactId>
    <version>3.9</version>
</dependency>
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>fastjson</artifactId>
    <version>1.2.62</version>
</dependency>
```

## getting started

具体demo可见[Demo类](<https://github.com/yamadadada/GDUTLoginUtil/blob/master/src/main/java/com/yamada/Demo.java>)



将GDUTLoginUtil工具类导入自己的项目中

初始化工具类，false表示是否启动SSL证书验证，由于教务系统使用了https，为了方便起见这里选择不验证证书

```java
GDUTLoginUtil loginUtil = GDUTLoginUtil.newInstance(false);
```

初始化后便可获取cookie

```java
loginUtil.getCookie();
```

### 获取登录验证码图片

```java
@GetMapping("/getImageCode")
public void getImageCode(HttpServletResponse response) {
	GDUTLoginUtil.getInstance().getImageCode(response);
}
```

getImageCode(response)能直接设置response响应头信息并返回验证码图片

### 登录

```java
loginUtil.login(account, password, verifycode);
```

参数account为学号，password为密码，verifycode为验证码

其中password需要事先在前端加密后传递给后端

#### 前端加密配置

前端使用了CryptoJS对密码进行加密

CryptoJS的导入方法可以前往CryptoJS主页查看：<https://github.com/brix/crypto-js>

该项目的resources下也导入了CryptoJS中必要的文件，可以直接用来使用

```javascript
<script src="aes.js"></script>
<script src="cipher-core.js"></script>
<script src="core.js"></script>
<script src="enc-base64.js"></script>
<script src="enc-utf8.js"></script>
<script src="evpkdf.js"></script>
<script src="hmac.js"></script>
<script src="mode-ecb.js"></script>
<script src="pad-pkcs7.js"></script>

<script type="text/javascript">
    var account = "";
    var password = "";
    var verifycode = "";
    var key = CryptoJS.enc.Utf8.parse(verifycode + verifycode + verifycode + verifycode);
    var srcs = CryptoJS.enc.Utf8.parse(password);
    var encrypted = CryptoJS.AES.encrypt(srcs, key, {mode:CryptoJS.mode.ECB, padding: CryptoJS.pad.Pkcs7});
    password = encrypted.ciphertext.toString();
</script>
```

具体可见[password-encode.html](<https://github.com/yamadadada/GDUTLoginUtil/blob/master/src/main/resources/password-encode.html>)

### 验证登录

```java
String response = loginUtil.login(account, password, verifycode);
if (loginUtil.isLoginSuccess(response)) {
	return "登录成功，cookie为：" + loginUtil.getCookie();
}
return "登录失败：" + loginUtil.getLoginMessage(response);
```

接下来就可以用登录成功的cookie愉快地玩耍啦

### 刷新cookie

如果遇到cookie失效的情况（login时返回信息已过期），可以使用如下方法刷新cookie（同时图形验证码也需要重新获取）

```java
GDUTLoginUtil.getInstance().refreshCookie();
```

