# 1장 IoC 컨테이너와 DI

## 1.1 IoC 컨테이너: 빈팩토리와 애플리케이션 컨텍스트

### 요약
스프링 애플리케이션에서는 오브젝트의 생성과 관계설정, 사용, 제거 등의 작업을 애플리케이션 코드 대신 독립된 컨테이너가 담당한다. 이를 컨테이너가 코드 대신 오브젝트에  
대한 제어권을 가지고 있다고 해서 IoC라고 부른다. 그래서 스프링 컨테이너를 IoC 컨테이너라고도 한다.  
스프링에선 IoC를 담당하는 컨테이너를 빈 팩토리 또는 애플리케이션 컨텍스트라고 부르기도 한다. 오브젝트의 생성과 오브젝트 사이의 런타임 관계를 설정하는 DI 관점으로  
볼 때는 컨테이너를 빈 팩토리라고 한다.  
DI를 위한 빈 팩토리에 엔터프라이즈 애플리케이션을 개발하는 데 필요한 여러 가지 컨테이너 기능을 추가한 것을 애플리케이션 컨텍스트라고 부른다. 즉 애플리케이션 컨텍스트는  
그 자체로 IoC와 DI를 위한 빈 팩토리이면서 그 이상의 기능을 가졌다.  
ApplicationContext 인터페이스는 BeanFactory의 서브인터페이스인 ListableBeanFactory와 HierarchicalBeanFactory를 상속한다.

```java
public interface ApplicationContext extends ListableBeanFactory, HierarchicalBeanFactory, MessasgeSource,  
    ApplicationEventPublisher, ResourcePatternResolver {
```

실제로 스프링 컨테이너 또는 IoC 컨테이너라고 말하는 것은 바로 이 ApplicationContext 인터페이스를 구현한 클래스 오브젝트다.  
스프링 애플리케이션은 최소한 하나 이상의 IoC 컨테이너, 즉 애플리케이션 컨텍스트 오브젝트를 갖고 있다.  

#### IoC 컨테이너를 이용해 애플리케이션 만들기
가장 간단하게 IoC 컨테이너를 만드는 방법은 다음과 같이 ApplicationContext 구현 클래스의 인스턴스를 만드는 것이다. 
```java
StaticApplicationContext ac = new StaticApplicationContext();
```

이렇게 만들어진 컨테이너가 본격적인 IoC 컨테이너로서 동작하려면 POJO 클래스와 설정 메타정보 두 가지가 필요하다.  

POJO 클래스  
먼저 애플리케이션의 핵심 코드를 담고 있는 POJO 클래스를 준비해야 한다. 지정된 사람에게 인사를 할 수 있는 Hello라는 클래스와 메시지를 받아서 이를 출력하는  
Printer 인터페이스를 구현한 StringPrinter 클래스를 만들자.

```java
public class Hello {
    String name;
    Printer printer;

    public String sayHello() {
        return "Hello " + name; 
    }
    
    public void print() {
        this.printer.print(sayHello());
    }
    
    // setter for name, printer
}

public interface Printer {
    void print(String message);
}

public class StringPrinter {
    private StringBuffer buffer = new StringBuffer();
    
    public void print(String message) {
        this.buffer.append(message);
    }
}
```

각자 기능에 충실하게 독립적으로 설계된 POJO 클래스를 만들고, 결합도가 낮은 유연한 관계를 가질 수 있도록 인터페이스를 이용해 연결해주는 것까지가 IoC 컨테이너가  
사용할 POJO 준비하는 첫 단계다.

설정 메타정보  
두 번째 필요한 것은 앞에서 만든 POJO 클래스들 중에 애플리케이션에서 사용할 것을 선정하고 이를 IoC 컨테이너가 제어할 수 있도록 적절한 메타정보를 만들어 제공하는  
작업이다. 

스프링의 설정 메타정보는 BeanDefinition 인터페이스로 표현되는 순수한 추상 정보다. 스프링 IoC 컨테이너, 즉 애플리케이션 컨텍스트는 바로 이 BeanDefinition  
으로 만들어진 메타정보를 담은 오브젝트를 사용해 IoC와 DI 작업을 수행한다. BeanDefinition 오브젝트로 변환해주는 BeanDefinitionReader가 있으면 된다.  
BeanDefinition 인터페이스로 정의되는, IoC 컨테이너가 사용하는 빈 메타정보는 대략 다음과 같다.  

- 빈 아이디, 이름, 별칭: 빈 오브젝트를 구분할 수 있는 식별자
- 클래스 또는 클래스 이름: 빈으로 만들 POJO 클래스 또는 서비스 클래스 정보 
- 스코프: 싱글톤, 프로토타입과 같은 빈의 생성 방식과 존재 범위 
- 프로퍼티 값 또는 참조: DI에 사용할 프로퍼티 이름과 값 또는 참조하는 빈의 이름
- 생성자 파라미터 값 또는 참조: DI에 사용할 생성자 파라미터 이름과 값 또는 참조할 빈의 이름
- 지연 로딩 여부, 우선 빈 여부, 자동와이어링 여부, 부모 빈 정보, 빈팩토리 이름 등

스프링 IoC 컨테이너는 각 빈에 대한 정보를 담은 설정 메타정보를 읽어들인 뒤에, 이를 참고해서 빈 오브젝트를 생성하고 프로퍼티나 생성자를 통해 의존 오브젝트를  
주입해주는 DI 작업을 수행한다. 이 작업을 통해 만들어지고, DI로 연결되는 오브젝트들이 모여서 하나의 애플리케이션을 구성하고 동작하게 된다. 

결국 스프링 애플리케이션이란 POJO 클래스와 설정 메타정보를 이용해 IoC 컨테이너가 만들어주는 오브젝트의 조합이라고 할 수 있다. 

Hello 클래스를 IoC 컨테이너 빈으로 등록하는 학습 테스트 코드다. 빈 메타정보의 항목들은 대부분 디폴트 값이 있다. 싱글톤으로 관리되는 빈 오브젝트를 등록할 때  
반드시 제공해줘야 하는 정보는 빈 이름과 POJO 클래스 뿐이다.

```java
StaticApplicationContext ac = new StaticApplicationContext();
ac.registerSingleton("hello1", Hello.class);

Hello hello1 = ac.getBean("hello1", Hello.class);
assertThat(hello1, is(notNullValue()));
```

이번에는 직접 BeanDefinition 타입의 설정 메타정보를 만들어서 IoC 컨테이너에 등록하는 방법을 사용해보자. RootBeanDefinition은 가장 기본적인   
BeanDefinition 인터페이스의 구현 클래스다.

```java
BeanDeifinition helloDef = new RootBeanDefinition(Hello.class);
helloDef.getProperyValues().addPropertyValue("name", "Spring");
ac.registerBeanDefinition("hello2", helloDef);
```

빈에 DI 되는 프로퍼티는 스트링이나 숫자 등의 값과 다른 빈 오브젝트를 가리키는 레퍼런스로 구분할 수 있다. 레퍼런스로 지정된 프로퍼티는 다른 빈 오브젝트를 주입해서  
오브젝트 사이의 관계를 만들어내는 데 사용된다. 

이번에는 Hello 타입의 빈과 StringPrinter 타입의 빈을 hello와 printer라는 빈 이름으로 생성하고 printer 빈이 hello 빈에게 DI 되도록 만들어보자.

```java
@Test
public void registerBeanWithDependency() {
    StaticApplicationContext ac = new StaticApplicatinContext();
    
    ac.registerBeanDefinition("printer", new RootBeanDefinition(StringPrinter.class));
    
    BeanDefinition helloDef = new RootBeanDefinition(Hello.class);
    helloDef.getPropertyValues().addPropertyValue("name", "Spring");
    helloDef.getPropertyValues().addPropertyValue("printer", new RuntimeBeanReference("printer"));
    
    ac.registerBeanDefinition("hello", helloDef);
    
    Hello hello = ac.getBean("hello", Hello.class);
    hello.print();
    
    assertThat(ac.getBean("printer").toString(), is("Hello Spring"));
    }
```

이렇게 애플리케이션을 구성하는 빈 오브젝트를 생성하는 것이 IoC 컨테이너의 핵심 기능이다. IoC 컨테이너는 일단 빈 오브젝트가 생성되고 관계가 만들어지면 그 뒤로는  
거의 관여하지 않는다. 기본적으로 싱글톤 빈은 애플리케이션 컨텍스트의 초기화 작업 중에 모두 만들어진다.  

#### IoC 컨테이너의 종류와 사용 방법
스프링 애플리케이션에서 직접 코드를 통해 ApplicationContext 오브젝트를 생성하는 경우는 거의 없다. 대부분 간단한 설정을 통해 ApplicationContext가  
자동으로 만들어지는 방법을 사용하기 때문이다. 먼저 스프링이 제공하는 ApplicationContext 구현 클래스에는 어떤 종류가 있고 어떻게 사용되는지 알아보자.

StaticApplicationContext  
StaticApplicationContext는 코드를 통해 빈 메타정보를 등록하기 위해 사용한다. 스프링의 기능에 대한 학습 테스트를 만들 때를 제외하면 실제로 사용되지 않는다.  

GenericApplicationContext  
가장 일반적인 애플리케이션 컨텍스트의 구현 클래스다. XML 파일과 같은 외부의 리소스에 있는 빈 설정 메타정보를 리더를 통해 읽어들여서 메타정보로 전환해서 사용한다.  
특정 포맷의 빈 설ㅈ어 메타정보를 읽어서 이를 애플리케이션 컨텍스트가 사용할 수 있는 BeanDefinition 정보로 변환하는 기능을 가진 오브젝트는 BeanDefinitionReader  
인터페이스를 구현해서 만들고, 빈 설정정보 리더라고 불린다. XML로 작성된 빈 설정정보를 읽어서 컨테이너에게 전달하는 대표적인 빈 설정정보 리더는  
XmlBeanDefinitionReader다. 이 리더를 GenericApplicationContext가 이용하도록 해서 hello 빈과 printer 빈을 등록하고 사용하게 만들어보자.

```java
@Test
public void genericApplicationContext() {
    GenericApplicationContext ac = new GenerinApplicationContext();
    
    XmlBeanDeifinitionReader reader = new XmlBeanDefinitionReader(ac);
    reader.loadBeanDefinitions("springbook/learningtest/spring/ioc/genericApplicationContext.xml");
    
    ac.refresh(); // 모든 메타정보가 등록이 완료됐으니 애플리케이션 컨테이너를 초기화한다.
    }
```

스프링은 XML 말고도 프러퍼티 파일에서 빈 설정 메타정보를 가져오는 PropertiesBeanDefinitionReader도 제공한다.  
빈 설정 리더를 만들기만 하면 어떤 형태로도 빈 설정 메타정보를 작설할 수 있다. DB의 테이블에 빈 설정정보를 저장해두고 이를 읽어서 사용하거나, 원격 서버로부터 정보를  
읽어올 수도 있다. 스프링에서는 대표적으로 XML 파일, 자바 소스코드 애노테이션, 자바 클래스 세 가지 방식으로 빈 설정 메타정보를 작성할 수 있다.  

GenericApplicationContext를 직접 사용할 일은 거의 없다. 스프링 테스트 컨텍스트 프레임워크를 활용하는 JUnit 테스트는 테스트 내에서 사용할 수 있도록 
애플리케이션 컨텍스트를 자동으로 만들어준다. 이때 생성되는 애플리케이션 컨텍스트가 바로 GenericApplicationContext다.

아래와 같이 테스트 클래스를 만들었다면 테스트가 실행되면서 GenericApplicationContext가 생성되고 @ContextConfiguration에 지정한 XML 파일로  
초기화돼서 테스트 내에서 사용할 수 있도록 준비된다. 

```java
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/test-applicationContext.xml")
public class UserServiceTest {
    @Autowired ApplicationContext applicationContext;
}
```

GenericXmlApplicationContext  
코드에서 GenericApplicationContext를 사용하는 경우에는 번거롭게 XmlBeanDefinitionReader를 직접 만들지 말고, 이 두 개의 클래스가 결합된  
GenericXmlApplicationContext를 사용하면 편리하다.  
GenericXmlApplicationContext는 XmlBeanDefinitionReader를 내장하고 있기 때문에 XML 파일을 읽어들이고 refresh()를 통해 초기화하는 것까지  
한 줄로 끝낼 수 있다. 

```java
GenericApplicationContext ac = new GenericXmlApplicationContext("xml file path");
```

WebApplicationContext  
스프링 애플리케이션에서 가장 많이 사용되는 애플리케이션 컨텍스트는 WebApplicationContext 인터페이스이다. 가장 많이 사용되는 구현체는 XML 설정파일을 사용하도록  
만들어진 XmlWebApplicationContext다. 애노테이션을 이용한 설정 리소스만 사용한다면 AnnotationApplicationContext를 쓰면 되고 deafult는  
XmlWebApplicationContext다.

스프링 IoC 컨테이너는 빈 설정 메타정보를 이용해 빈 오브젝트를 만들고 DI 작업을 수행한다. 하지만 그것만으로는 애플리케이션이 동작하지 않는다. 어디에선가 특정 빈  
오브젝트의 메소드를 호출함으로써 애플리케이션을 동작시켜야 한다. 그래서 간단히 스프링 애플리케이션을 만들고 IoC 컨테이너를 직접 셋업했다면 다음과 같은 코드가 반드시  
등장한다. 

```java
ApplicationContext ac = ...
Hello hello = ac.getBean("hello", Hello.class);
hello.print();
```

적어도 한 번은 IoC 컨테이너에게 요청해서 빈 오브젝트를 가져와야 한다. 이때는 필히 getBean()이라는 메소드를 사용해야 한다. 

그러나 웹 애플리케이션은 동작하는 방식이 근본적으로 다르다. 웹에서는 main() 메소드를 호출할 방법이 없기 때문에 서블릿이 일종의 main() 메소드와 같은 역할을 한다.  
웹 애플리케이션에서 스프링 애플리케이션을 가동하기 위해서는 main() 메소드 역할을 하는 서블릿을 만들어두고, 미리 애플리케이션 컨텍스트를 생성해둔 다음, 요청이  
서블릿으로 들어올 때마다 getBean()으로 필요한 빈을 가져와 정해진 메소드를 실핼해주면 된다. 

스프링은 웹 환경에서 애플리케이션 컨텍스트를 생성하고 설정 메타 정보로 초기화해주고, 클라이언트로부터 들어오는 요청마다 적절한 빈을 찾아서 이를 실행해주는 기능을 가진  
DispatcherServlet이라는 이름의 서블릿을 제공한다. 스프링이 제공해준 서블릿을 web.xml에 등록하는 것만으로 웹 환경에서 스프링 컨테이너가 만들어지고 애플리케이션을  
실행하는 데 필요한 대부분의 준비는 끝이다.  

#### IoC 컨테이너 계층구조
빈을 담아둔 IoC 컨테이너는 애플리케이션마다 하나씩이면 충분하다. 하지만 트리 모양의 계층구조를 만들 때 한 개 이상의 IoC 컨테이너를 만들어두고 사용해야할 경우가 있다.  

부모 컨텍스트를 이용한 계층구조 효과  
모든 애플리케이션 컨텍스트는 부모 애플리케이션 컨텍스트를 가질 수 있다. 계층구조 안의 모든 컨텍스트는 각자 독립적인 설정정보를 이용해 빈 오브젝트를 만들고 관리한다.  
각자 독립적으로 자신이 관리하는 빈을 갖고 있긴 하지만 DI를 위해 빈을 찾을 때는 부모 애플리케이션 컨텍스트의 빈까지 모두 검색한다. 먼저 자신이 관리하는 빈 중에서  
필요한 빈을 찾아보고, 없으면 부모 컨텍스트에게 빈을 찾아달라고 요청한다.  
검색 순서는 항상 자신이 먼저이고, 그런 다음 직계 부모 순서이다.

미리 만들어진 애플리케이션 컨텍스트의 설정을 그대로 가져다가 사용하면서 그중 일부 빈만 설정을 변경하고 싶다면, 애플리케이션 컨텍스트를 두 개 만들어서 하위 컨텍스트에서  
바꾸고 싶은 빈들을 다시 설정해줘도 된다. 이런 경우 계층구조를 만드는 방법이 편리하다. 

컨텍스트 계층구조 테스트  

```java
ApplicationContext parent = new GenericXmlApplicationContext(basePath + "parentContext.xml");
// 세밀한 컨텍스트 설정을 위해서는 GenericApplicationContext를 이용해햐 한다.
GenericApplicationContext child = new GenericApplicationContext(parent); 

XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(child);
reader.loadBeanDefinitions(basePath + "childContext.xml");
child.refresh();
```

#### 웹 애플리케이션의 IoC 컨테이너 구성
스프링은 서블릿이 중앙집중식으로 모든 요청을 다 받아서 처리하는 프론트 컨트롤러 패턴 방식을 사용한다. 웹 애플리케이션 안에서 동작하는 IoC 컨테이너는 두 가지 방법으로  
만들어진다. 하나는 스프링 애플리케이션의 요청을 처리하는 서블릿 안에서 만들어지는 것이고, 다른 하나는 웹 애플리케이션 레벨에서 만들어지는 것이다. 일반적으로는 이 두  
가지 방식을 모두 사용해 컨테이너를 만든다. 그래서 스프링 웹 애플리케이션에는 두 개의 WebApplicationContext 오브젝트가 만들어진다.

웹 애플리케이션의 컨텍스트 계층구조  
하나의 웹 애플리케이션 내에 두 개의 스프링 서블릿이 존재하는 경우에 서블릿 A와 서블릿 B는 각각 자신의 애플리케이션 컨텍스트를 갖고 있다. 동시에 두 컨텍스트가  
공유해서 사용하는 빈을 담아놓을 수 있는 별도의 컨텍스트가 존재한다. 이 컨텍스트는 각 서블릿에 존재하는 컨텍스트의 부모 컨텍스트로 만든다. 스프링에서 애플리케이션  
컨텍스트 계층구조가 사용되는 가장 대표적인 경우다.  
일반적으로는 스프링의 애플리케이션 컨텍스트를 가지면서 프론트 컨트롤러 역할을 하는 서블릿은 하나만 만들어 사용한다. 그런데 여러 개의 자식 컨텍스트를 두고 공통적인  
빈을 부모 컨텍스트로 뽑아내서 공유하려는 게 아니라면 왜 이렇게 계층구조로 만들까? 그 이유는 전체 애플리케이션에서 웹 기술에 의존적인 부분과 그렇지 않은 부분을 구분하기  
위해서다.

웹 애플리케이션의 컨텍스트 구성 방법  
웹 애플리케이션의 애플리케이션 컨텍스트를 구성하는 방법으로 다음 세 가지를 고려해볼 수 있다. 첫 번째 방법은 컨텍스트 계층구조를 만드는 것이고, 나머지 두 가지 방법은  
컨텍스트를 하나만 사용하는 방법이다. 첫 번째와 세 번째 방법은 스프링 웹 기능을 사용하는 경우이고, 두 번째 방법은 스프링 웹 기술을 사용하지 않을 때 적용 가능한  
방법이다. 

- 서블릿 컨텍스트와 루트 애플리케이션 컨텍스트 계층구조
- 루트 애플리케이션 컨텍스트 단일구조
- 서블릿 컨텍스트 단일구조

루트 애플리케이션 컨텍스트 등록  
서블릿 애플리케이션 컨텍스트 등록


## 1.2 IoC/DI를 위한 빈 설정 메타정보 작성  

### 요약
#### 빈 설정 메타 정보
BeanDefinition에는 IoC 컨테이너가 빈을 만들 때 필요한 핵심 정보가 담겨 있다. BeanDefinition은 여러 개의 빈을 만드는 데 재사용될 수 있다. 따라서  
BeanDefinition에는 빈의 이름이나 아이디를 나타내는 정보는 포함되지 않는다. 대신 IoC 컨테이너에 이 BeanDefinition 정보가 등록될 때 이름을 부여해줄 수 있다.  

빈 설정 메타정보 항목 중에서 가장 중요한 것은 클래스 이름임다. 추상 빈으로 정의하지 않는 한 클래스 정보는 반드시 필요하다. 빈은 오브젝트이고, 오브젝트를 생성하려면  
반드시 클래스가 필요하기 때문이다. 컨테이너에 빈의 메타정보가 등록될 때 꼭 필요한 것은 클래스 이름과 함께 빈의 아이디 또는 이름이다. 

#### 빈 등록 방법
빈 등록은 빈 메타정보를 작성해서 컨테이너에게 건내주면 된다. 보통 XML 문서, 프로퍼티 파일, 소스코드 애노테이션과 같은 외부 리소스로 빈 메타정보를 작성하고 이를  
적절한 리더나 변환기를 통해 애플리케이션 컨텍스트가 사용할 수 있는 정보로 변환해주는 방법을 선택한다.  
스프링에서 자주 사용되는 빈의 등록 방법은 크게 다섯 가지가 있다.  

1. XML: <bean> 태그  
```java
<bean id ="hello" class="springbook.learningtest.spring.ioc.bean.Hello">
...
</bean>
```
DI이긴 하지만 특정 빈과 강한 결합을 가지고 등록되는 경우에 내부 빈을 사용할 수 있다. 


2. XML: 네임스페이스와 전용 태그  
스프링은 기술적인 설정과 기반 서비스를 빈으로 등록할 때를 위해 의미가 잘 드러나는 네임스페이스와 태그를 가진 설정 방법을 제공한다. <bean> 태그로 등록된 포인트컷은  
<aop:pointcut> 태그로 바꿀 수 있다. 네임스페이스와 전용 태그, 전용 애트리뷰트를 이용해 선언됐기 때문에 내용이 매우 분명하게 드러나고 선언 자체도 깔끔해진다.


3. 자동인식을 이용한 빈 등록: 스테레오타입 애노테이션과 빈 스캐너  
XMl 문서와 같이 한곳에 명시적으로 선언하지 않고도 스프링 빈을 등록하는 방법이 있다. 빈으로 사용될 클래스에 특별한 애노테이션을 부여해주면 이런 클래스를 자동으로  
찾아서 빈으로 등록해주게 할 수 있다. 이렇게 특정 애노테이션이 붙은 클래스를 자동으로 찾아서 빈으로 등록해주는 방식을 빈 스캐닝을 통한 자동인식 빈 등록기능이라고 하고,  
이런 스캐닝 작업을 담당하는 오브젝트를 빈 스캐너라고 한다. @Component 애노테이션이 붙은 또는 @Component 애노테이션을 메타 애노테이션으로 가진 클래스를 선택한다.  
@Component를 포함해 디폴트 필터에 적용되는 애토네이션을 스프링에서는 스테레오타입 애노테이션이라고 부른다.
빈 스캐너는 기본적으로 클래스 이름의 앞글자를 소문자로 바꾸어 빈의 아이디로 사용한다.
AnnotationConfigApplicationContext는 빈 스캐너를 내장하고 있는 애플리케이션 컨텍스트 구현체이다.=
자동스캔을 사용하면 XML 문서 생성과 관리에 따른 수고를 덜어주고 개발 속도를 향상시킬 수 있지만 정의를 한눈에 파악하기는 힘들기도 하다. 또 빈 스캔에 의해 자동등록되는  
빈은 XML처럼 상세한 메타정보 항목을 지정할 수 없고, 클래스당 한 개 이상의 빈을 등록할 수 없다는 제한이 있다. 


4. 자바 코드에 의한 빈 등록: @Configuration 클래스의 @Bean 메소드  
오브젝트 생성과 의존관계 주입을 담당하는 오브젝트를 오브젝트 팩토리라고 불렀고, 오브젝트 팩토리의 기능을 일반화해서 컨테이너로 만든 것이 지금의 스프링 컨테이너, 즉 
빈팩토리라고 볼 수 있다. 
자바 코드에 의한 빈 등록 기능은 하나의 클래스 안에 여러 개의 빈을 정의할 수 있다. 또 애노테이션을 이용해 빈 오브젝트의 메타정보를 추가하는 일도 가능하다.  
게다가 그 정의를 담고 있는 클래스 자체가 자동인식 빈의 대상이 되기 때문에 XML을 통해 명시적으로 등록하지 않아도 된다. 빈 설정 메타정보를 담고 있는 자바 코드는
@Configuration 애노테이션이 달린 클래스를 이용해 작성한다. 
자바 코드를 이용한 빈 등록에 사용되는 클래스는 그저 평범한 자바 코드처럼 동작하지 않는다는 사실을 알아야 한다. 스프링은 @Bean이 붙은 메소드를 이용해서 빈을 만들 때  
   싱글톤 빈이라면 한 개의 오브젝트만 생성이 되고 더 이상 새로운 오브젝트가 만들어지지 않도록 특별한 방법으로 @Bean 메소드를 조작해둔다. 이 때문에 annotatedHello()  
   메소드를 다시 실행했더라도 처음 만들어진 annotatedHello 빈 오브젝트가 반복적으로 리턴되는 것이다. @Configuration과 @Bean을 사용하는 클래스는 순수한  
   오브젝트 팩토리 클래스라기보다는 자바 코드로 표현되는 메타정보라고 이해하는 것이 좋다. 
   
자바 코드에 의한 설정이 XML과 같은 외부 설정파일을 이용하는 것보다 유용한 점을 몇가지 살펴보자. 

- 컴파일러나 IDE를 통한 타입 검증이 가능하다.  
XML은 텍스트 문서이기 때문에 클래스 이름의 오류나 프로퍼티 이름을 잘못적어도 쉽게 검증할 수 없다. 반면에 자바코드는 컴파일러나 IDE를 통해 쉽게 검증이 가능하다. 
  
- 자동완성과 같은 IDE 지원 기능을 최대한 이용할 수 있다 

- 이해하기 쉽다

- 복잡한 빈 설정이나 초기화 작업을 손쉽게 적용할 수 있다
빈 오브젝트 생성과 초기화 작업이 복잡한 경우가 있다. new 키워드 대신 스태틱 팩토리 메소드 등을 통해 빈 오브젝트를 생성할 수도 있다. 
  
5. 자바 코드에 의한 빈 등록: 일반 빈 클래스의 @Bean 메소드  
@Configuration이 붙은 클래스가 아닌 일반 POJO 클래스에도 @Bean을 사용할 수 있다. 물론 @Bean 메소드를 가진 클래스는 어떤 방법으로든지 빈으로 등록돼야 한다.  
   @Configuration 클래스 안에서 싱글톤으로 빈이 생성되던 것은 POJO 클래스 내에서는 해당하지 않기 때문에 매번 다른 Printer 오브젝트를 받게 되므로 주의해야  
   한다. 일반적으로 @Bean 메소드를 통해 정의되는 빈이 클래스로 만들어진 빈과 매우 밀접하게 관계가 있는 경우, 특별히 종속적인 빈인 경우에 사용하자. 
   
#### 빈 의존관계 설정 방법
DI 할 대상을 선정한느 방법으로 분류해보면 명시적으로 구체적인 빈을 지정하는 방법과 일정한 규칙에 따라 자동으로 선정하는 방법으로 나눌 수 있다. 보통 전잔느 DI할  
빈의 아이디를 직접 지정하는 것이고, 후자는 주로 타입 비교를 통해서 호환되는 타입의 빈을 DI 후보로 삼는 방법이다. 후자의 방법은 보통 자동와이어링이라고 불린다.  

XML: <property>, <constructor-arg>  
<bean>을 이용해 빈을 등록했다면 프로퍼티와 생성자 두 가지 방식으로 DI를 지정할 수 있다. 프로퍼티는 자바빈 규약을 따르는 수정자 메소드를 사용하고, 생성자는 빈  
클래스의 생성자를 이용하는 방법이다. 두 가지 방법 모두 파라미터로 의존 오브젝트 또는 값을 주입해준다.

- < property>: 수정자 주입
```xml
<bean ...>
    <property name="printer" ref="defaultPrinter" />
</bean>

<bean id="defaultPrinter" class="...">
```

- <constructor-arg>: 생성자 주입
생성자 주입은 생성자의 파라미터를 이용하기 때문에 한 번에 여러 개의 오브젝트를 주입할 수 있다. 파라미터의 순서나 타입을 명시하는 방법이 필요하다.
```xml
<bean id="hello" class="springbook.learningtest.spring.ioc.bean.Hello">
    <constructor-arg index="0" value="Spring" />
    <constructor-arg index="1" ref="printer" />
</bean>
```

파라미터에 중복타입이 없다면 타입으로 지정할수도 있다.

```xml
    <constructor-arg type="java.lang.String" value="Spring" />
    <constructor-arg type="springbook.learningtest.spring.ioc.bean.Printer" ref="printer" />
```

XML: 자동와이어링  
- byName: 빈 이름 자동와이어링
```xml
<bean id="hello" class=""...Hello" autowire="byName">
    <property name="name" value="Spring" />
</bean>
```

printer 프로퍼티 선언은 생략됐지만 <bean>의 옵션으로 준 autowire="byName"에 의해 스프링은 Hello 클래스의 프로퍼티의 이름과 동일한 빈을 찾아서 자동으로  
프로퍼티로 등록해준다. <beans>의 디폴트 자동와이어링 옵션을 설정하면 전체 <bean>에 적용할 수 있다. 자동와이어링이 어려운 프로퍼티 값이나 특별한 이름을 가진  
프로퍼티의 경우에는 명시적으로 <property>를 선언해주면 된다.

- byType: 타입에 의한 자동와이어링  
타입에 의한 자동와이어링은 autowire="byType"을 넣어주면 된다. 하지만 타입에 의한 자동와이어링은 타입이 같은 빈이 두 개 이상 존재하는 경우에는 적용되지 못한다.  
  이름에 의한 자동와이어링보다 느리다는 단점도 있다.  
  XML 안에서 자동와이어링을 사용하는 경우 XML만 봐서는 빈 사이의 의존관계를 알기 힘들다는 단점이 있다. 이름을 이용한 자동와이어링에서는 오타로 빈 이름을 잘못 적어서  
  DI 되지 않고 넘어갈 위험도 있다. 타입에 의한 자동와이어링은 대입 가능한 타입이 두 개 이상이면 문제가 된다. 또, 하나의 빈에 대해 한 가지 자동와이어링 방식 밖에  
  지정할 수 없다는 사실도 자동 와이어링 방식의 한계다.
  
XML: 네임스페이스와 전용 태그 

애노테이션: @Resource  
@Resource는 <property> 선언과 비슷하게 주입할 빈을 아이디로 지정하는 방법이다. @Resource는 자바 클래스의 수정자뿐만 아니라 필드에도 붙일 수 있다. 
@Resource 애노테이션을 사용하면 수정자 메소드가 없어도 직접 내부 필드에 DI 할 수가 있다.
수정자에 @Resource 애노테이션으로 주입받을 수 있으며 수정자가 없이도 필드에 @Resource 애노테이션을 적용하여 바로 필드 주입을 해줄 수도 있다. XML의  
자동와이어링은 각 프로퍼티에 주입할 만한 후보 빈이 없을 경우 무시하고 넘어가기 때문에 느슨한 방법이다. 반면에 @Resource는 자동와이어링처럼 프로퍼티 정보를 코드와  
관례를 이용해서 생성해내지만 DI 적용 여부를 프로퍼티마다 세밀하게 제어할 수 있다는 점에서 다르다. DI 할 빈을 찾을 수 없다면 예외가 발생한다.  
@Resource에 엘리먼트를 지정하지 않는 경우 디폴트 이름으로 참조할 빈을 찾을 수 없는 경우 타입을 이용해 다시 한 번 빈을 찾기도 한다. 

애노테이션: @Autowired/@Inject  
@Autowired는 XML의 타입에 의한 자동와이어링 방식을 생성자, 필드, 수정자 메소드, 일반 메소드의 네가지로 확장한 것이다. 

- 수정자 메소드와 필드
@Autowired 애노테이션이 부여된 필드나 수정자를 만들어주면 스프링이 자동으로 DI 해주도록 만드는 것이다. @Resource와 다른 점은 이름 대신 필드나 프로퍼티 타입을  
  이용해 후보 빈을 찾는다는 것이다. 
  
- 생성자 
@Autowired는 @Resource와 다르게 생성자에도 부여할 수 있다. 
  
- 일반 메소드
생성자는 주입은 한 번 생성되면 DI를 변경하기 힘들다. 그래서 등장한 것 중의 하나가 바로 일반 메소드를 사용하는 DI 방법이다. 
  
- 컬렉션과 배열  
@Autowired를 이용하면 같은 타입의 빈이 하나 이상 존재할 때 그 빈들을 모두 DI 받도록 할 수 있다. @Autowired의 대상이 되는 필드나 프로퍼티, 메소드의 파라미터  
  를 컬렉션이나 배열로 선언하면 된다.
```java
@Autowired
Collection<Printer> printers; // Set<>, List<> 가능

@Autowired
Printer[] printers;

@Autowired
Map<String, Printer> printerMap;
```
의도적으로 타입이 같은 여러 개의 빈을 등록하고 이를 모두 참조하거나 그중에서 선별적으로 필요한 빈을 찾을 때 사용하는 것이 좋다. 

@Qualifier
Qualifier는 타입 외의 정보를 추가해서 자동와이어링을 세밀하게 제어할 수 있는 보조적인 방법이다. 
```java
@Autowired
@Qualifier("mainDB")
DataSource dataSource;
```

스프링은 @Qualifier를 메타 애노테이션으로 갖는 애노테이션도 @Qualifier 취급을 해준다. 그래서 @Database라는 이름으로 새로운 한정자 애노테이션을 만들 수 있다.  
이름을 이용해 빈을 지정하고 싶다면 @Resource를 사용하고, 타입과 한정자를 활용하고 싶을 때만 @Autowired를 사용하는 것이 바람직하다.  
@Qualifier는 그 부여 대상이 필드와 수정자, 파라미터 뿐이다. 생성자와 일반 메소드 경우에는 의미가 없다. 이때는 각 파라미터에 직접 @Qualifer를 붙이면 된다.  
타입에 의한 자동와이어링으로 빈을 찾을 수 없더라도 상관없다면, @Autowired의 required 엘리먼트를 false로 설정하면 된다. 

@Autowired와 getBean(), 스프링 테스트  
@Autowired는 스프링에서 가장 유연하면서 가장 강력한 기능을 가진 의존관계 설정방법이다. 특정 타입의 빈이 하나만 존재한다면 
Printer printer = ac.getBean(Printer.class); 로 빈을 찾을 수 있다.

자바 코드에 의한 의존관계 설정  
- 애노테이션에 의한 설정 @Autowired, @Resource
빈은 자바 코드에 의해 생성되지만 의존관계는 빈 클래스의 애노테이션을 이용하게 할 수 있다. @Autowired와 같은 애노테이션을 통한 의존관계 설정은 빈 오브젝트 등록을  
  마친 후에 후처리기에 의해 별도의 작업으로 진행된다.
  
- @Bean 메소드 호출
@Configuration과 @Bean을 사용하는 자바 코드 설정 방식의 기본은 메소드로 정의된 다른 빈을 메소드 호출을 통해 참조하는 것이다.
  
- @Bean과 메소드 자동와이어링
메소드로 정의된 다른 빈을 가져와 자바 코드로 의존정보를 생성할 때 직접 @Bean이 붙은 메소드를 호출하는 대신 그 빈의 레퍼런스를 파라미터로 주입받는 방식을 사용할 수  
  있다. @Bean이 붙은 메소드는 기본적으로 @Autowired가 붙은 메소드처럼 동작한다.
  
#### 프로퍼티 값 설정 방법

메타정보 종류에 따른 값 설정 방법  
- XML: <property>와 전용 태그
ref 대신 value를 사용한다면 런타임 시에 주입할 값으로 인식한다.
  
- 애노테이션: @Value
필드나 생성자에 미리 값을 초기화하지 않는 이유는 환경에 따라 매번 달라질 수도 있고, 외부에서 초기값대신 다른 값을 지정하고 싶기 때문이다.  
  @Value 애노테이션의 주요 용도는 자바 코드 외부의 리소스나 환경정보에 담긴 값을 사용하도록 지정해주는데 있다. 
  
- 자바 코드: @Value
@Configuration에서 @Value를 주입받을 수도 있고, @Bean 메소드의 파라미터에 @Value를 직접 사용할 수도 있다. 
  
#### 컨테이너가 자동등록하는 빈  
ApplicationContext, BeanFactory  
컨테이너 자신을 빈으로 등록해두고 필요하면 일반 빈에서 DI 받아서 사용할 수 있다. ApplicationContext의 구현 클래스는 기본적으로 BeanFactory의 기능을  
직접 구현하고 있지 않고 내부에 빈 팩토리 오브젝트를 별도로 만들어두고 위임하는 방식을 사용하기 때문에 컨텍스트 내부에 만들어진 빈 팩토리 오브젝트를 직접 사용하고  
싶다면 BeanFactory 타입으로 DI를 하면 된다.

ResourceLoader, ApplicationEventPublisher  
코드를 통해 서블릿 컨텍스트의 리소스를 읽어오고 싶다면 컨테이너를 ResourceLoader 타입으로 DI 받아서 사용하면 된다.

systemProperties, systemEnvironment  
스프링 컨테이너가 직접 등록하는 빈 중에서 타입이 아니라 이름을 통해 접근할 수 있는 두 가지 빈이 있다. systemProperties 빈과 systemEnvironment 빈이다.  
각각 Propeties 타입과 Map 타입이기 때문에 타입에 의한 접근 방법은 적절치 않다.  
systemProperties 빈은 System.getProperties() 메소드가 돌려주는 Properties 타입의 오브젝트를 읽기 전용의 오브젝트로 접근할 수 있게 만든 빈  
오브젝트다.


## 1.3 프로토타입과 스코프
기본적으로 스프링 빈은 싱클톤으로 만들어진다. 따라서 싱글톤의 필드에는 의존관계에 있는 빈에 대한 레퍼런스나 읽기전용 값만 저장해두고 오브젝트의 변하는 상태를 저장하는  
인스턴스 변수는 두지 않는다. 때로는 빈이 싱글톤 방식이 아닌 다른 방법으로 만들어져야될 때가 있다. 싱글톤이 아닌 빈은 프로토타입과 빈 스코프 빈이다.

스코프  
스코프는 존재할 수 있는 범위를 가리키는 말이다. 빈의 스코프는 빈 오브젝트가 만들어져 존재할 수 있는 범위다.

#### 프로토 타입 스코프
@Scope("prototype") 을 통해 프로토타입 빈을 만들 수 있고 컨테이너에 빈을 요청할 때마다 새로운 오브젝트를 만든다.

프로토타입 빈의 생명주기와 종속성  
IoC의 기본 개념은 애플리케이션을 구성하는 핵심 오브젝트를 코드가 아니라 컨테이너가 관리힌다는 것이다. 그래서 스프링이 관리하는 오브젝트인 빈은 그 생성과 다른 빈에  
대한 의존ㄷ성 주입, 초기화, DI와 DL을 통한 사용, 제거에 이르기 ㄷ까지 모든 오브젝트의 생명주기를 컨테이너가 관리한다. 하지만 프로토타입 빈은 컨테이너가 빈을  
제공하고 나면 컨테이너는 더 이상 빈 오브젝트를 관리하지 않는다. 따라서 프로토타입 빈은 한번 DL이나 DI를 통해 컨테이너 밖으로 전달된 후에는 이 오브젝트는 더 이상  
스프링이 관리하는 빈이 아니다. 그래서 프로토타입 빈은 이 빈을 주입받은 오브젝트에 종속적일 수 밖에 없다.  

프로토타입 빈의 용도  
프로토타입 빈은 코드에서 new로 오브젝트 생성하는 것을 대신하기 위해 사용된다. 사용자의 요청별로 독립적인 정보나 작업 상태를 저장해둘 오브젝트를 만들 필요가 있다.  
대부분 new 키워드나 팩토리를 이용해 코드 안에서 직접 오브젝트를 만들면 된다. new로 충분한 작업을 번거롭게 컨테이너에게 요청할 필요는 없다. 그런데 드물긴 하지만  
컨테이너가 오브젝트를 만들고 초기화해줘야 하는 경우가 존재한다. 바로 DI 때문이다.  
매번 새롭게 만들어지는 오브젝트가 컨테이너 내의 빈을 사용해야 하는 경우가 있다. 오브젝트에 DI를 적용하려면 컨테이너가 오브젝트를 만들게 해야한다. 이런 경우에  
프로토타입 빈이 유용하다. 매번 새로운 오브젝트가 필요하면서 DI를 통해 다른 빈을 사용할 수 있어야 한다면 프로토타입 빈이 가장 적절한 선택이다.  
고객의 A/S 접수를 저장하고 메일을 발송하는 서비스를 개발할 때 고객의 정보를 ServiceRequest 객체를 new 로 생성하여 서비스 계층에 전달하고, 서비스 계층에서  
DAO를 통해 customer를 받아 올 수 도 있지만 ServiceRequest를 프로토타입 빈으로 만들고 DAO를 DI 받아 Customer를 가져오는 일을 ServiceRequest에서  
하게 되면 데이터 중심에서 오브젝틀 중심으로 바꿀 수 있다. 마찬가지고 메일을 보내는 것도 ServiceRequest 내에서 처리할 수 있다. 컨트롤러 계층에서  
ApplicatoinContext를 주입받고 getBean()을 사용하면 매번 새로운 ServiceRequest를 받아올 수 있다. 

DI와 DL  
앞서서 getBean()을 이용해 DL을 사용하였다. 번거롭게 DL 방식을 쓰지 않고 프로토타입 빈을 직접 DI 해서 사용하는 건 어떨까? 컨트롤러에서 @Autowired를 사용해  
ServiceRequest를 받아오게 한다면 문제가 생길 것이다. 컨트롤러는 싱글톤 빈이기 때문에 생성할 때 딱 한번만 ServiceRequest 빈을 가져온다. 따라서 코드 내에서  
필요할 때마다 컨테이너에게 요청해서 새로운 오브젝트를 만들어야한다. 즉 DL 방식을 사용해야 한다. 

프로토타입 빈의 DL 전략  
getBean()을 사용하는 방법은 스프링의 API가 일반 애플리케이션에 코드에서 사용하게 된다. 스프링은 프로토타입 빈처럼 DL 방식을 코드에서 사용해야 할 경우를 위해  
직접 ApplicationContext를 이용하는 것 외에도 다양한 방법을 제공한다.

- ApplicationContext, BeanFactory
기존에 사용했던 방법
  
- ObjectFactory, ObjectFactoryCreatingFactoryBean
직접 애플리케이션 컨텍스트를 사용하지 않으려면 중간에 컨텍스트에 getBean()을 호출해주는 역할을 맡은 오브젝트를 두면 된다. 
  
```java
@Resource
private ObjectFactory<ServiceRequest> serviceReqeustFactory;

public void serviceReqeustFormSubmit(HttpServletReqeust request) {
    ServiceRequest serviceRequest = this.serviceRequestFactory.getObject();
    serviceRequest.setCustomerByCustomerNo(request.getParameter("custno"));
}
```

- ServiceLocatorFactoryBean
ObjectFactory가 단순하고 깔끔하지만 프레임워크 인터페이스를 애플리케이션 코드에서 사용하는 것이 맘에 들지 않을 수도 있다. 이럴땐 ServiceLocatorFactoryBean을  
  사용하면 된다. 
  
```java
public interface ServiceRequestFactory {
    ServiceRequest getServiceFactory();
}
// ServiceLocatorFactoryBean으로 등록

@Autowired ServiceRequestFactory serviceRequestFactory;

public void serviceRequestFormSubmit(HttpServletReqeust request) {
    ServiceRequest serviceRequest = this.serviceRequestFactory.getServiceFactory();
}
```

- 메소드 주입
메소드 주입은 메소드를 통한 주입이 아니라 메소드 코드 자체를 주입하는 것을 말한다. 메소드 주입은 일정한 규칙을 따르는 추상 메소드를 작성해두면 ApplicationContext와  
  getBean() 메소드를 사용해서 새로운 프로토타입 빈을 가져오는 기능을 담당하는 메소드를 런타임 시에 추가해주는 기술이다.
  
```java
abstract public ServiceRequest getServiceReqeust();
```

```xml
<bean id="serviceRequestController" class="...ServiceRequestController">
    <lookup-method name="getServiceRequest" bean="serviceRequest" />
</bean>
```
고급 방법이긴 하지만 단위테스트에서 단점이 더 많을 수도 있다. 

- Provider<T>
JSR-330에 추가된 표준 인터페이스인 Provider가 있다. Provider는 ObjectFactory와 거의 유사하게 <T> 타입 파라미터와 get()이라는 팩토리 메소드를 가진  
  인터페이스다. Provider 인터페이스를 @Inject, @Autowired, @Resource 중의 하나를 이용해 DI 되도록 지정해주기만 하면 스프링이 자동으로 Provider를  
  구현한 오브젝트를 생성해서 주입해준다.
```java
@Inject Provider<ServiceRequest> serviceRequestProvidor;

public void serviceReqeustFormSubmit(HttpServletRequest reqeust) {
    ServiceRequest serviceReqeust = this.serviceRequestProvider.get();
    }
```

Provider를 사용하는 것이 가장 좋다. 싱글톤 빈은 학습 테스트를 만드는 용도 외에는 DL을 사용해야 할 필요는 없다. 

#### 스코프
스코프의 종류  
스프링은 싱글톤, 프로토타입 외에 요청, 세션, 글로벌세션, 애플리케이션이라는 네 가지 스코프를 기본적으로 제공한다. 애플리케이션을 제외한 나머지 세 가지 스코프는  
싱글톤과 다르게 독립적인 상태를 저장해두고 사용하는데 필요하다. 서버에서 만들어지는 빈 오브젝트에 상태를 저장해둘 수 있는 이유는 사용자마다 빈이 만들어지는 덕분이다.

- 요청 스코프
요청 스코프는 빈이 하나의 웹 요청 안에서 만들어지고 해당 요청이 끝날 때 제거된다. 존재 번위와 특징은 하나의 요청이 일어나는 메소드 파라미터로 전달되는 도메인 오브젝트나  
  DTO와 비슷하다. 
  
- 세션 스코프, 글로벌 세션 스코프
HTTP 세션과 같은 존재 범위를 갖는 빈으로 만들어주는 스코프다. HTTP 세션은 사용자별로 만들어지고 브라우저를 닫거나 세션 타임이 종료될 때까지 유지되기 때문에  
  로그인 정보나 사용자별 선택옵션 등을 저장해두기에 유용하다.
  
-애플리케이션 스코프
애플리케이션 스코프는 서블릿 컨텍스트에 저장되는 빈 오브젝트다. 서블릿 컨텍스트는 웹 애플리케이션마다 만들어진다. 웹 애플리케이션마다 스프링의 애플리케이션 컨텍스트도  
만들어진다. 웹 애플리케이션과 애플리케이션 컨텍스트의 존재 범위가 다른 경우가 있기 때문에 사용된다. 

스코프 빈의 사용 방법  
애플리케이션 스코프를 제외한 나머지 세 가지 스코프는 DI를 통해 사용할 수 없고 DL을 사용해야 한다. 하지만 스프링이 제공하는 특별한 DI 방법을 사용하면 DI처럼  
사용할 수 있다. 직접 스코프 빈을 DI 하는 대신 스코프 빈에 대한 프록시를 DI 해주는 것이다.


