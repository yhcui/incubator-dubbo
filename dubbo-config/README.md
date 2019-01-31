## 配置模块
* 是 Dubbo 对外的 API，用户通过 Config 使用Dubbo，隐藏 Dubbo 所有细节。

## 作用
* 用户都是使用配置来使用dubbo，dubbo也提供了四种配置方式，包括XML配置、属性配置、API配置、注解配置，配置模块就是实现了这四种配置的功能

## 模块
### dubbo-config-api
* 实现了API配置和属性配置的功能

### dubbo-config-spring
* 实现了XML配置和注解配置的功能

## 与spring结合
