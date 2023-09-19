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



