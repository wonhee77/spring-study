# 6장 AOP

AOP는 IoC/DI, 서비스 추상화와 더불어 스프링의 3대 기반기술의 하나다. 스프링에 적용된 가장 인기 있는 AOP의 적용 대상은 바로 선언적 트랜잭션 기능이다.  
서비스 추상화를 통해 많은 근본적인 문제를 해결했던 트랜잭션 경계설정 기능을 AOP를 이용해 더욱 세련되고 깔끔한 방식으로 바꿔보자. 그리고 그 과정에서 스프링이 AOP를  
도입해야 했던 이유도 알아보자.  

## 6.1 트랜잭션 코드의 분리

### 요약
지금까지 서비스 추상화 기법을 통해 트랜잭션 기술에 독립적으로 만들어줬고, 메일 발송 기술과 환경에도 종속적이지 않게 깔끔한 코드로 다듬어 왔지만 트랜잭션 경계설정을 위해  
넣은 코드들이 찜찜하다. 스프링이 제공하는 깔끔한 트랜잭션 인터페이스를 썼음에도 비즈니스 로직이 주인이어야 할 메소드 안에 이름도 길고 무시무시하게 생긴 트랜잭션 코드가  
더 많은 자리를 차지하고 있다.

기존의 코드를 살펴보면 비즈니스 로직 코드를 사이에 두고 트랜잭션 시작과 종료를 담당하는 코드가 앞뒤에 위치하고 있다.  
그리고 트랜잭션 경계설정의 코드와 비즈니스 로직 코드 간에 서로 주고받는 정보가 없다.  

레벨을 업그레이드하는 비즈니스 로직을 upgradeLevelsInternal() 메소드로 분리하자.  

기존에는 UserServiceTest가 UserService 구체적인 구현 클래스를 직접 참조하고 있기 때문에 결합도가 강하다. UserService를 인터페이스로 만들고 기존 코드는  
UserService 인터페이스 구현 클래스를 만들어넣도록 한다.  

한 번에 두 개의 UserServuce 인터페이스 구현클래스를 동시에 이용한다면 어떻까? 지금 해결하려고 하는 문제는 UserService에는 순수하게 비즈니스 로직을 담고 있는 코드만  
놔두고 트랜잭션 경계설정을 담당하는 코드를 외부로 빼내려는 것이다.  

UserServiceTx라는 UserService를 구현한 또 다른 구현 클래스를 만든다. 이 클래스는 단지 트랜잭션의 경계설정이라는 책임을 맡고 있다. 그리고 스스로는 비즈니스 로직을  
담고 있지 않기 때문에 또 다른 비즈니스 로직을 담고 있는 UserService의 구현 클래스에 실제적인 로직 처리 작업을 위임하는 것이다.  

UserService의 add(), upgradeLevel() 메소드는 인터페이스의 추상메소드로 남겨두고 기존의 UserService를 UserServiceImpl로 변경한다.  
트랜잭션 관련 코드는 모두 제거한다.  

이제 비즈니스 트랜잭션 처리를 담은 UserServiceTx를 만들어보자. UserServiceTx는 기본적으로 UserSerivce를 구현하게 만든다. 그리고 같은 인터페이스를 구현한 다른  
오브젝트에게 고스란히 작업을 위임하게 만들면 된다. UserServiceTx는 사용자 관리라는 비즈니스 로직을 전혀 갖지 않고 고스란히 다른 UserService 구현 오브젝트에 기능을 위임한다.  
이를 위해 UserService 오브젝트를 DI 받을 수 있도록 만든다.

이렇게 준비된 UserServiceTx에 트랜잭션의 경계설정이라는 부가적인 작업을 부여해보자. 구체적인 기술을 알지 못하지만 transactionManager라는 이름의 빈으로 등록된  
트랜잭션 매니저를 DI로 받아뒀다가 트랜잭션 안에서 동작하도록 만들어줘야 하는 메소드 호출의 전과 후에 필요한 트랜잭션 경계설정 API를 사용해주면 된다.  

이제 남은 건 설정파일을 수정해주는 부분이다. 기존에 UserService 빈이 의존하고 있던 transactionManager는 UserServiceTx의 빈이, userDao와 MailSender는  
UserServiceImpl 빈이 각각 의존하도록 프로퍼티 정보를 분리한다.  

#### 트랜잭션 분리에 따른 테스트 수정
기존의 @Autowired 어노테이션으로 주입하던 UserService는 interface로 변경되었기 때문에 수정할 것이 없다. 하지만 UserService 타입의 빈이 2개이기 때문에  
어떤 빈이 주입될지 알지 못한다. @Autowired는 기본적으로 타입을 이용해 빈을 찾지만 만약 타입으로 하나의 빈을 결정할 수 없는 경우에는 필드 이름을 이용해 빈을 찾는다.

@Autowired UserService userService; // 타입이 UserService이고 id가 userService인 빈을 찾아서 주입

다음으로 UserServiceImpl 빈을 주입해야한다. 단순히 UserService의 기능을 테스트할 때는 인터페이스로 구체적인 클래스 정보를 노출할 필요가 없지만  
이렇게 목 오브젝트를 이용해 수동 DI를 적용하는 테스트라면 어떤 클래스의 오브젝트인지 분명하게 알 필요가 있다.

@Autowired UserServiceImpl userServiceImpl;

#### 트랜잭션 경계설정 코드 분리의 장점
트랜잰션 경계설정 코드의 분리와 DI를 통한 연결의 장점  
첫째, 이제 비즈니스 로직을 담당하고 있는 UserServiceImpl의 코드를 작성할 때는 트랜잭션과 같은 기술적인 내용에는 전혀 신경 쓰지 않아도 된다.  
트랜잭션은 DI를 이용해 UserServiceTx와 같은 트랜잭션 기능을 가진 오브젝트가 먼저 실행되도록 만들기만 하면 된다.  
두번째 장점은 비즈니스 로직에 대한 테스트를 손쉽게 만들어낼 수 있다는 것이다.  


## 6.2 고립된 단위 테스트 

### 요약 
가장 편하고 좋은 테스트 방법은 가능한 작은 단위로 쪼개서 테스트하는 것이다. 작은 단위의 테스트가 좋은 이유는 테스트가 실패했을 때 그 원일을 찾기 쉽기 때문이다.  
또한 테스트 단위가 작아야 테스트의 의도나 내용이 분명해지고, 만들기도 쉬워진다. 테스트 대상의 단위가 커지면 충분한 테스트를 만들기도 쉽지 않다.  

UserService에서 테스트 단위를 쪼개지 않았다면 그 뒤의 의존관계를 따라 등장하는 오브젝트와 서비스, 환경 등이 모두 합쳐져 테스트 대상이 되는 것이다.  

그래서 테스트의 대상이 환경이나, 외부 서버, 다른 클래스의 코드에 종속되고 영향을 받지 않도록 고립시킬 필요가 있다. 테스트를 의존 대상으로부터 분리해서 고립시키는 방법은  
MailSender에 적용해봤던 대로 테스트를 위한 대역을 사용하는 것이다. MailSender에는 이미 DummyMailSender라는 테스트 스텁을 적용했다. 또 테스트 대역이 테스트 검증에도  
참여할 수 있도록, 특별히 만든 MockMailSender라는 목 오브젝트로 사용해봤다.  

UserDao는 단지 테스트 대상의 코드가 정상적으로 수행되도록 도와주기만 하는 스텁이 아니라, 부가적인 검증 기능까지 가진 목 오브젝트로 만들었다. 그 이유는 고립된 환경에서  
동작하는 upgradeLevels()의 테스트 결과를 검증할 방법이 필요하기 때문이다. 이 메소드는 리턴 타입이 void이기 때문에 테스트가 제대로 동작하는지 검증하는 방법은 DB의  
값을 확인한느 방법 뿐이다. 그러나 고립테스트에서는 DB를 사용할 수 없다. 그래서 Mock객체를 만들어 넘겨받은 인자를 통해 테스트의 동작을 확인하였다.  

이 책에서는 앞으로 upgradeLevels() 테스트처럼 '테스트 대상 클래스를 목 오브젝트 등의 테스트 대역을 이용해 의존 오브젝트나 외부의 리소스를 사용하지 않도록 고립시켜  
테스트하는 것'을 단위테스트라고 부르겠다. 반면에 두 개 이상의, 성격이나 계층의 다른 오브젝트가 연동하도록 만들어 테스트하거나, 또는 외부의 DB나 파일, 서비스 등의  
리소스가 참여하는 테스트는 통합 테스트라고 부르겠다.

단위 테스트와 통합 테스트 중 어떤 방법을 쓸지 결정할 가이드라인  
- 항상 단위 테스트를 먼저 고려한다. 
- 하나의 클래스나 성격과 목적이 같은 긴밀한 클래스 몇 개를 모아서 외부와의 의존관계를 모두 차단하고 필요에 따라 스텁이나 목 오브젝트 등의 테스트 대역을 이용하도록 테스트를 만든다. 
- 외부 리소스를 사용해야만 가능한 테스트는 통합테스트로 만든다.
- DAO는 그 자체로 로직을 담고 있기 보다는 DB를 통해 로직을 수행하는 인터페이스와 같은 역할을 한다. 따라서 DAO는 DB까지 연동하는 테스트로 만드는 편이 효과적이다. 
- 여러 개의 단위가 의존관계를 가지고 동작할 때를 위한 통합 테스트는 필요하다. 다만, 단위 테스트를 충분히 거쳤다면 통합 테스트의 부담은 상대적으로 줄어든다.
- 단위 테스트를 만들기가 너무 복잡하다고 판단되는 코드는 처음부터 통합 테스트를 고려해본다.
- 스프링 테스트 컨텍스트 프레임워크를 이용하는 테스트는 통합 테스트다. 스프링의 설정 자체도 테스트 대상이고, 스프링을 이용해 좀 더 추상적인 레벨에서 테스트가 필요할 경우  
스프링 테스트 컨텍스트 프레임워크를 이용해 통합 테스트를 작성한다.  
  
코드를 작성하면서 테스트는 어떻게 만들 수 있을까를 생각해보는 것은 좋은 습괸이다. 테스트하기 편하게 만들어진 코드는 깔끔하고 좋은 코드가 될 가능성이 높다.  

단위 테스트를 만들기 위해서는 스텁이나 목 오브젝트의 사용이 필수적이다. 목 오브젝트는 사용하지 않는 인터페이스도 모두 일일이 구현해야되기 때문에 번거롭다.  
다행히도, 이런 번거로운 목 오브젝트를 편리하게 작성하도록 도와주는 다양한 목 오브젝트 지원 프레임워크가 있다.  

#### Mockito 프레임워크
Mockito와 같은 목 프레임워크의 특징은 목 클래스를 일일이 준비해둘 필요가 없다. org.mockito.Matchers 클래스의 mock() 메소드를 스태틱 임포트를 사용해  
로컬 메소드처럼 호출하면 편리하다.

```java
UserDao mockUserDao = mock(UserDao.class);
```

getAll() 메소드가 불려올 때 사용자 목록을 리턴하도록 스텁기능을 추가해줘야 한다.

```java
when(mockUserDao.getAll()).thenReturn(this.users);
```

Mockito 목 오브젝트는 다음의 네 단계를 거쳐서 사용하면 된다.  
- 인터페이스를 이용해 목 오브젝트를 만든다.
- 목 오브젝트가 리턴할 값이 있으면 이를 지정해준다. 메소드가 호출되면 예외를 강제로 던지게 만들 수 있다. 
- 테스트 대상 오브젝트에 DI해서 목 오브젝트가 테스트 중에 사용되도록 만든다. 
- 테스트 대상 오브젝트를 사용한 후에 목 오브젝트의 특정 메소드가 호출됐는지, 어떤 값을 가지고 몇번 호출됐는질를 검증한다. 


## 6.3 다이내믹 프록시와 팩토리 빈

### 요약
UserServiceTx를 이용해서 UserServiceImpl의 부가기능과 핵심기능을 분리할 수 있었다. 문제는 이렇게 구성했더라도 클라이언트가 핵심기능을 가진 클래스를 직접 사용해 버리면  
부가기능이 적용될 기회가 없다는 점이다. 그래서 부가기능은 마치 자신이 핵심 기능을 가진 클래스인 것처럼 꾸며서, 클라이언트가 자신을 거쳐서 핵심기능을 사용하도록 만들어야 한다.  
그러기 위해서는 클라이언트는 인터페이스를 통해서만 핵심기능을 사용하게 하고, 부가기능 자신도 같은 인터페이스를 구현한 뒤에 자신이 그 사이에 까어들어야 한다.  
이렇게 마치 자신이 클라이언트가 사용하려고 하는 실제 대상인 것처럼 위장해서 클라이언트의 요청을 받아주는 것을 대리자, 대리인과 같은 역할을 한다고 해서 프록시라고 부른다.  
그리고 프록시를 통해 최종적으로 요청을 위임받아 처리하는 실제 오브젝트를 타깃 또는 실체라고 부른다.  

프록시는 사용 목적에 따라 두 가지로 구분할 수 있다. 첫째는 클라이언트가 타깃에 접근하는 방법을 제어하기 위해서다. 두번째는 타깃에 부가적인 기능을 부여해주기 위해서다.  
두 가지 모두 대리 오브젝트라는 개념의 프록시를 두고 사용한다는 점은 동일하지만, 목적에 따라서 디자인 패턴에서는 다른 패턴으로 구분한다.  

#### 데코레이터 패턴
데코레이터 패턴은 타깃에 부가적인 기능을 런타임 시 다이내믹하게 부여해주기 위해 프록시를 사용하는 패턴을 말한다. 다이내믹하게 기능을 부가한다는 의미는 컴파일 시점,  
즉 코드상에서는 어떤 방법과 순서로 프록시와 타깃이 연결되어 사용되는지 정해져 있지 않다는 뜻이다. 이 패턴의 이름이 데코레이터라고 불리는 이유는 마치 제품이나 케익 등을 여러 겹으로 포장하고  
그 위에 장식을 붙이는 것처럼 실제 내용물은 동일하지만 부가적인 효과를 부여햐줄 수 있기 때문이다. 따라서 데코레이터 패턴에서는 프록시가 꼭 한개로 제한되지 않는다.  

인터페이스를 통한 데코레이터 정의와 런타임 시의 다이내믹한 구성 방법은 스프링의 DI를 이용하면 아주 편리하다. 데코레이터 빈의 프로퍼티로 같은 인터페이스를 구현한 다른 데코레이터  
또는 타깃 빈을 설정하면 된다.

데코레이터 패턴은 타깃의 코드를 손대지 않고, 클라이언트가 호출하는 방법도 변경하지 않은 채로 새로운 기능을 추가할 때 유용한 방법이다. 

#### 프록시 패턴
일반적으로 사용하는 프록시라는 용어와 디자인 패턴에서 말하는 프록시 패턴은 구분할 필요가 있다. 전자는 클라이언트와 사용 대상 사이에 대리 역할을 맡은 오브젝트를 두는 방법을 총칭한다면,  
후자는 프록시를 사용하는 방법 중에서 타깃에 대한 접근 방법을 제어하려는 목적을 가진 경우를 가리킨다.  

프록시 패턴의 프록시는 타깃의 기능을 확장하거나 추가하지 않는다. 대신 클라이언트가 타깃에 접근하는 방식을 변경해준다. 타깃 오브젝트를 생성하기가 복잡하거나 당장 필요하지 않은 경우에는  
꼭 필요한 시점까지 오브젝트를 생성하지 않는 편이 좋다. 그런데 타깃 오브젝트에 대한 레퍼런스가 미리 필요할 수 있다. 이럴 때 프록시 패턴을 적용하면 된다.  
클라이언트에게 타깃에 대한 레퍼런스를 넘겨야 하는데, 실제 타깃 오브젝트를 만드는 대신 프록시를 넘겨주는 것이다. 그리고 프록시의 메소드를 통해 타깃을 사용하려고 시도하면,  
그때 프록시가 타깃 오브젝트를 생성하고 요청을 위임해주는 식이다.  

구조적으로 보자면 프록시와 데코레이터는 유사하다. 다만 프록시는 코드에서 자신이 만들거나 접근할 타깃 클래스 정보를 알고 있는 경우가 많다.

#### 다이내믹 프록시
목 프레임워크를 사용했던 것처럼 프록시도 일일이 모든 인터페이스를 구현해서 클래스를 새로 정의하지 않고도 편리하게 만들어서 사용할 방법은 없을까?

자바에는 java.lang.reflect 패키지 안에 프록시를 손쉽게 만들 수 있ㄷ도록 지원해주는 클래스들이 있다. 일일이 프록시 클래스를 정의하지 않고도 몇 가지 API를 이용해  
프록시처럼 동작하는 오브젝트를 다이내믹하게 생성하는 것이다.  

#### 프록시의 구성과 프록시 작성의 문제점
프록시는 다음의 두 가지 기능으로 구성된다.  
- 타깃과 같은 메소드를 구현하고 있다가 메소드가 호출되면 타깃 오브젝트로 위임한다.
- 지정된 요청에 대해서는 부가기능을 수행한다.

프록시를 만들기가 번거로운 이유는 무엇일까?
첫번째, 타깃의 인터페이스를 구현하고 위임하는 코드를 작성하기가 번거롭다는 점이다. 부가기능이 필요 없는 메소드도 구현해서 타깃으로 위임하는 코드를 일일이 만들어줘야한다.  
또, 타깃 인터페이스의 메소드가 추가되거나 변경될 때마다 함께 수정해줘야 한다.  
두번째 문제점은 부가기능 코드가 중복될 가능성이 많다는 점이다.

#### 리플렉션
다이내믹 프록시는 리플렉션 기능을 이용해서 프록시를 만들어준다. 리플렉션은 자바의 코드 자체를 추상화해서 접근하도록 만든 것이다.  
자바의 모든 클래스는 그 클래스 자체의 구성정보를 담은 Class타입의 오브젝트를 하나씩 갖고 있다. '클래스이름.class'라고 하거나 오브젝트의 getClass() 메소드를 호출하면  
클래스 정보를 담은 Class 타입의 오브젝트를 가져올 수 있다. 클래스 오브젝트를 이용하면 클래스 코드에 대한 메타정보를 가져오거나 오브젝트를 조작할 수 있다.  
예를 들어 클래스의 이름이 무엇이고, 어떤 클래스를 상속하고, 어떤 인터페이스를 구현했는지, 어떤 필드를 갖고 있고, 각각의 타입은 무엇인자, 메소드는 어떤 것을 정의했고,  
메소드의 파라미터와 리턴 타입은 무엇인지 알아낼 수 있다. 더 나아가서 오브젝트 필드의 값을 읽고 수정할 수도 있고, 원하는 파라미터 값을 이용해 메소드를 호출할 수도 있다.  

클래스이름.class 나 오브젝트가 있다면 name.getClass()를 통해 얻은 객체에 .getMethod()를 호출하면 메소드 정보를 가져올 수 있다.
```java
    String.class.getMethod("length");
```

java.lang.reflect.Method 인터페이스는 메소드에 대한 정보를 담고 있을 뿐만 아니라, 이를 이용해 특정 오브젝트의 메소드를 실행시킬 수도 있다.  
Method 인터페이스에 정의된 invoke() 메소드를 사용하면 된다. invoke() 메소드는 메소드를 실행시킬 대상 오브젝트(obj)와 파라미터 목록(args)을 받아서  
메소드를 호출한 뒤에 그 결과를 Object 타입으로 돌려준다.
```java
    public Object invoke(Object obj, Object... args)

    ex)
    String name = "Spring";
    Method chatAtMethod = String.class.getMethod("chatAt", int.class);
    assertThat((Character) chatAtMethod.invoke(name, 0), is('S'));
```

#### 프록시 클래스
다이내믹 프록시를 이용한 프록시를 만들어보자. 

```java
interface Hello {
    String sayHello(Sting name);
    String sayHi(String name);
    String sayThankYou(String name);
}

public class HelloTarget implements Hello {
    public String sayHello(Sting name){
        return "Hello " + name;
    };
    public String sayHi(String name){
        return "Hi " + name;
    };
    public String sayThankYou(String name){
        return "Thank You " + name;
    };
}
```

테스트
```java
@Test
public void simpleProxy() {
    Hello hello = new HelloTarget(); // 타깃은 인터페이스를 통해 접근하는 습관을 들이자.
    assertThat(hello.sayHello("Toby"), is("Hello Toby"));
    assertThat(hello.sayHi("Toby"), is("Hi Toby"));
    assertThat(hello.sayThankYou("Toby"), is("Thank You Toby"));
    }
```

이제 Hello 인터페이스를 구현한 프록시를 만들어보자. 프록시에는 데코레이터 패턴을 적용해서 타깃인 HelloTarget에 부가기능을 추가하겠다.  
프록시 이름은 HelloUppercase다. 추가할 기능은 리턴하는 문자를 모두 대문자로 바꿔주는 것이다.

```java
public class HelloUppercase implements Hello {
    Hello hello; // 위임할 타깃 오브젝트, 여기서는 타깃 클래스의 오브젝트인 것은 알지만 다른 프록시를 추가할 수도 있으므로 인터페이스로 접근한다.
    
    public HelloUppercase(Hello hello) {
        this.hello = hello;
    }
    
    public String sayHello(Sting name){
        return hello.sayHello(name).toUpperCase();
    };
    public String sayHi(String name){
        return hello.sayHi(name).toUpperCase();
    };
    public String sayThankYou(String name){
        return hello.sayThankYou(name).toUpperCase();
    };
}
```

이 프록시는 프록시 적용의 일반적인 문제점 두 가지를 모두 갖고 있다. 인터페이스는 모든 메소드를 구현해 위임하도록 코드를 만들어야 하며, 부가기능인 리턴 값을 대문자로  
바꾸는 기능이 모든 메소드에 중복돼서 나타난다.  

#### 다이내믹 프록시 적용
클래스로 만든 프록시인 HelloUppercase를 다이내믹 프록시를 이용해 만드어보자. 다이내믹 프록시는 프록시 팩토리에 의해 런타임 시 다이내믹하게 만들어지는 오브젝트다.  
다이내믹 프록시 오브젝트는 타깃의 인터페이스와 같은 타입으로 만들어진다. 프록시 팩토리에게 인터페이스 정보만 제공해주면 해당 인터페이스를 구현한 클래스의 오브젝트를  
자동으로 만들어준다.  

다이내믹 프록시가 인터페이스 구현 클래스의 오브젝트는 만들어주지만, 프록시로서 필요한 부가기능 제공 코드는 직접 작성해야 한다. 부가기능은 프록시 오브젝트와 독립적으로  
InvocationHandler를 구현한 오브젝트에 담는다. InvocationHandler 인터페이스는 다음과 같은 메소드를 한 개만 가진 인터페이스이다.

```java
public Object invoke(Object proxy, Method method, Object[] args)
```

invoke() 메소드는 리플렉션의 Method 인터페이스를 파라미터로 받는다. 메소드를 호출할 때 전달되는 파라미터도 args로 받는다. 다이내믹 프록시 오브젝트는 클라이언트의 모든 요청을  
리플렉션 정보로 변환해서 InvocationHandler 구현 오브젝트의 invoke() 메소드로 넘기는 것이다.  

Hello 인터페이스를 제공하면서 프록시 팩토리에게 다이내믹 프록시를 만들어달라고 요청하면 Hello 인터페이스의 모든 메소드를 구현한 오브젝트를 생성해준다.  
InvocationHandler 인터페이스를 구현한 오브젝트를 제공해주면 다이내믹 프록시가 받은 모든 요청을 InvocationHandler의 invoke 메소드로 보내준다.  
Hello 인터페이스의 메소드가 아무리 많더라도 invoke() 메소드 하나로 처리할 수 있다. 

다이내믹 프록시를 만들어보자. 먼저 다이내믹 프록시로부터 메소드 호출 정보를 받아서 처리하는 InvocationHandler를 만들어보자.  

```java
public class UppercaseHandler implements InvocationHandler {
    Hello target;
    
    public UppercaseHandler(Hello target) {
        this.target = target;
    }
    
    public Object invoke(Object proxy, Method method, Object[] args) thorws Throwable {
        String ret = (String) method.invoke(target, args); // 타겟으로 위임, 인터페이스의 메소드 호출에 모두 적용된다.
        return ret.toUpperCase(); // 부가 기능 제공
    }
}
```

이제 이 InvocationHandler를 사용하고 Hello 인터페이스를 구현하는 프록시를 만들어보자. 다이내믹 프록시의 생성은 Proxy 클래스의 newProxyInstance()  
스태틱 팩토리 메소드를 이용하면 된다. 

```java
Hello proxiedHello = (Hello) Proxy.newProxyInstance(
    getCalss.getClassLoader(), // 동적으로 생성되는 다이내믹 프록시 클래스의 로딩에 사용할 클래스 로더
    new Class[] {Hello.class}, // 구현할 인터페이스
    new UppercaseHandler(new HelloTarget())); // 부가기능과 위임 코드를 담은 InvocationHandler
    )
```

첫번째 파라미터는 클래스 로더를 제공해야한다. 다이내믹 프록시가 정의되는 클래스 로더를 지정하는 것이다.  
두번째 파라미터는 다이내믹 프록시가 구현해야할 인터페이스이다. 다이내믹 프록시는 한 번에 하나 이상의 인터페이스를 구현할 수도 있다.  
마지막 파라미터로는 부가기능과 위임 관련 코드를 담고 있는 InvocationHandler 구현 오브젝트를 제공해야 한다.  

#### 다이내믹 프록시의 확장
InvocationHandler의 장점은 타깃의 종류에 상관없이도 적용이 가능하다는 점이다. 어차피 리플렉션의 Method 인터페이스를 이용해 타깃의 메소드를 호출하는 것이니  
Hello 타입의 타깃으로 제한할 필요도 없다.   
InvocationHandler는 단일 메소드에서 모든 요청을 처리하기 때문에 어떤 메소드에 어떤 기능을 적용할지를 선택하는 과정이 필요할 수도 있다.  
타깃 오브텍트의 모든 메소드에 트랜잭션을 적용하는게 아니라 선별적으로 적용할 것이므로 적용할 대상을 선별하는 작업을 먼저 진행한다.  

#### TransactionHandler와 다이내믹 프록시를 이용하는 테스트 
```java
@Test
public void upgradeAllOrNothing() throws Exception {
    ...
    TransactionHandler txHandler = new TransctionHandler();
    txHandler.setTarget(testUSerService);
    txHandelr.setTransctionManager(transctionManager);
    txHAndler.setPattern("upgradeLevels");
    
    UserService txUserService = (UserService) Proxy.newProxyInstance(
        getClass().getClassLoader(), new Class[] { UserService.class}, txHandler);
    ...
    )
}
```

이제 TransactionHandler와 다이내믹 프록시를 스프링의 DI를 통해 사용할 수 있도록 만들어야 할 차례다. 그런데 문제는 DI의 대상이 되는 다이내믹 프록시 오브젝트는 일반적인 스프링의  
빈으로는 등록할 방법이 없다는 점이다. 스프링의 빈은 기본적으로 클래스 이름과 프로퍼티로 정의된다. 클래스의 이름을 갖고 있다면 다음과 같은 방법으로 새로운 오브젝트를 생성할 수 있다.  
Class의 newInstance() 메소드는 해당 크래스의 파라미터가 없는 생성자를 호출하고, 그 결과 생성되는 오브젝트를 돌려주는 리플렉션 API다.  
```java
Date now = (Date) Class.forName("java.util.Date").newInstance();
```
스프링은 내부적으로 리플렉션 API를 이용해서 빈 정의에 나오는 클래스 이름을 가지고 빈 오브젝트를 생성한다. 문제는 다이내믹 프록시 오브젝트는 이런 식으로 프록시 오브젝트가 생성되지 않는다는 점이다.  
사실 다이내믹 프록시 오브젝트의 클래스가 어떤 것인지 알 수도 없다. 클래스 자체도 내부적으로 다이내믹하게 새로 정의해서 사용하기 때문이다. 따라서 사전에 프록시 오브젝트 클래스 정보를  
미리 알아내서 스프링의 빈에 정의할 방법이 없다. 다이내믹 프록시는 Proxy 클래스의 newProxyInstance()라는 스태틱 팩토리 메소드를 통해서만 만들 수 있다.  

#### 팩토리 빈
사실 스프링은 클래스 정보를 가지고 디폴트 생성자를 통해 오브젝트를 만드는 방법 이외도 빈을 만들 수 있는 여러 가지 방법을 제공한다.  
대표적으로 팩토리 빈을 이용한 빈 생성 방법을 들 수 있다. 팩토리 빈이란 스프링을 대신해서 오브젝트의 생성로직을 담당하도록 만들어진 특별한 빈을 말한다.  

팩토리빈은 FactoryBean 인터페스를 구현하여 만들 수 있다.
```java
package org.springframework.beans.factory;

public interface FactoryBean<T> {
    T getObject() throws Exception; // 빈오브젝트를 생성해서 돌려준다.
    Class<? extends  T> getObjectType(); // 생성되는 오브젝트의 타입을 알려준다.
    boolean isSingleton(); // getObject()가 돌려주는 오브젝트가 항상 같은 싱글톤 오브젝트인지 알려준다.
}
```

FactoryBean 인터페이스를 구현한 클래스를 스프링의 빈으로 등록하면 팩토리 빈으로 동작한다.  

#### 팩토리 빈의 설정 방법
```xml
<bean id="message" class="springbook.learningtest.spring.factorybean.MessageFactoryBean">
    <property name="text" value="Factory Bean" />
</bean>
```
여타 빈 설정과 다른 점은 message 빈 오브젝트의 타입이 class 애트리뷰트에 정의된 MessageFactoryBean이 아니라 message타입이라는 것이다.  
Message 빈의 타입은 MessageFactoryBean의 getObjectType() 메소드가 돌려주는 타입으로 결정된다. 또, getObject() 메소드가 생성해주는 오브젝트가 message 빈의 오브젝트가 된다.  

#### 다이내믹 프록시를 만들어주는 팩토리 빈
Proxy의 newProxyInstance() 메소드를 통해서만 생성이 가능한 다이내믹 프록시 오브젝트는 일반적인 방법으로는 스프링의 빈으로 등록할 수 없다.  
대신 팩토리 빈을 사용하면 다이내믹 프록시 오브젝트를 스프링의 빈으로 만들어줄 수가 있다. 팩토리 빈의 getObject() 메소드에 다이내믹 프록시 오브젝트를 만들어주는  
코드를 넣으면 되기 때문이다.

#### 트랜잭션 프록시 팩토리 빈
TransactionHandler를 이용하는 다이내믹 프록시를 생성하는 팩토리 빈 클래스다.
```java
package springbook.user.service;

public class TxProxyBean implements FactoryBean<Object> {
    Object target;
    PlatformTransactionManager transactionManager;
    String pattern;
    Class<?> serviceInterface;
    
    //setter for target, transactionManager, pattern, serviceInterface
    
    // FactoryBean 인터페이스 구현 메소드
    public Object getObject() throws Exception {
        TransactionHandler txHandler = new TrasctionHandler();
        txHandler.setTarget(target);
        txHandler.setTransactionManager(transactionManager);
        txHandler.setPattern(pattern);
        return Proxy.getProxyInstance(
            getCalss().getClassLoader, new Class[] { serviceInterface}, txHandler);)
    }
    
    public Class<?> getObjectType() {
        return serviceInterface; // 팩토리 빈이 생성하는 오브젝트 타입은 DI 받은 인터페이스에 타입에 따라 달라진다. 재사용 가능!
    }
    
    public boolean isSingleton() {
        return false; // 싱글톤 빈이 아니라는 뜻이 아니라 getObject()가 매번 같은 오브젝트를 리턴하지 않는다는 의미다.
    }
}
```

테스트 코드에서 updateAllOrNothing() 에서는 exception을 throw하는 TextUserService를 사용했다.  
TransactionHandler와 다이내믹 프록시 오브젝트를 직접 만들어서 테스트했을 때는 타깃 오브젝트를 바꾸기가 쉬웠는데, 이제는 스프링 빈에서 생성되는 프록시 오브젝트에 대해  
테스트를 해야 하기 때문에 간단하지 않다. 가장 문제는 타깃 오브젝트에 대한 레퍼런스는 TransactionHandler 오브젝트가 갖고 있는데, TransactionHandler는  
TxProxyFactoryBean 내부에서 만들어져 다이내믹 프록시 생성에 사용될 뿐 별도로 참조할 방법이 없다는 점이다.  

TxProxyFactoryBean의 트랜잭션을 지원하는 프록시를 바르게 만들어주는지를 확인하는 게 목적이므로 빈으로 등록된 TxProxyFactoryBean을 직접 가져와서 프록시를 만들 수 있다.  
```java
@Autowired ApplicationContext context;

// 팩토리 빈 자체를 가져와야 하므로 빈 이름에 &를 반드시 넣어야 한다.
TxProxyFactoryBean txProxyFactoryBean = context.getBean("&userService", TxProxyFactoryBean.class);
txProxyFactoryBean.setTarget(testUserService);
UserService txUserService = (UserService) txProxyFactoryBean.getObject();
```

#### 프록시 팩토리 빈 방식의 장점과 한계
다이내믹 프록시를 생성해주는 팩토리 빈을 사용하는 방법은 여러 가지 장점이 있다. 한번 부가기능을 가진 프록시를 생성하는 팩토리 빈을 만들어두면 타깃의 타입에 상관없이  
재사용할 수 있기 때문이다.  

장점
다이내믹 프록시를 이용하면 타깃 인터페이스를 구현하는 클래스를 일일이 만드는 번거로움을 제거할 수 있다. 하나의 핸들러 메소드를 구현하는 것 만으로도 수많은 메소드에 부가기능을  
부여해줄 수 있으니 부가기능 코드의 중복 문제도 사라진다. 프록시에 팩토리 빈을 이용한 DI까지 더해주면 번거로운 다이내믹 프록시 생성 코드도 제거할 수 있다.  
이 과정에서 스프링의 DI는 매우 중요한 역할을 했다. 프록시를 사용하려면 DI가 필요한 것은 물론이고 효율적인 프록시 생성을 위한 다이내믹 프록시를 사용하려고 할 때도  
팩토리 빈을 통한 DI는 필수다. 앞으로 살펴보겠지만 프록시 팩토리 빈을 좀 더 효과적으로 사용하고자 할 때도 DI가 중요한 역할을 한다.

한계
프록시를 통해 타깃에 부가기능을 제공하는 것은 메소드 단위로 일어나는 일이다. 하나의 클래스 안에 존재하는 여러 개의 메소드에 부가기능을 한 번에 제공하는 건 어렵지 않게 가능했다.  
하지만 한 번에 여러 개의 클래스에 공통적인 부가기능을 제공하는 일은 지금까지 살펴본 방법으로는 불가능하다. 하나의 타깃 오브젝트에만 부여되는 부가기능이라면 상관 없겠지만,  
트랜잭션과 같이 비즈니스 로직을 담은 많은 클래스 메소드에 적용할 필요가 있다면 거의 비슷한 프록시 팩토리 빈의 설정이 중복되는 것을 막을 수 없다.  
하나의 타깃에 여러 개의 부가기능을 적용하려고 할 때도 문제다. 적용 대상인 서비스 클래스가 200개쯤 된다면 보통 하나당 3~4줄이면 되는 서비스 빈의 설정에 5~6줄씩 되는 프록시 팩토리 빈 설정이  
부가기능의 개수만큼 따라 붙어야 한다.  
또 한 가지 문제점은 TransactionHandler 오브젝트가 프록시 팩토리 빈 개수만큼 만들어진다는 것이다. TransactionHandler는 타깃 오브젝트를 프로퍼티로 갖고 있다.  
따라서 트랜잭션 부가기능을 제공하는 동일한 코드임에도 불구하고 타깃 오브젝트가 달라지면 새로운 TransactionHandler 오브젝트를 만들어야 한다.  


## 6.4 스프링의 프록시 팩토리 빈
지금까지 기존 코드의 수정 없이 트랜잭션 부가기능을 추가해줄 수 있는 다양한 방법을 살펴봤다. 이제 스프링은 이러한 문제에 어떤 해결책을 제시하는지 살펴볼 차례다.  

#### ProxyFactoryBean
스프링은 트랜잭션 기술과 메일 발송 기술에 적용했던 서비스 추상화를 프록시 기술에도 동일하게 적용하고 있다. 자바에는 JDK에서 제공하는 다이내믹 프록시 외에도 편리하게  
프록시를 만들 수 있도록 지원해주는 다양한 기술이 존재한다. 따라서 스프링은 일관된 방법으로 프록시를 만들 수 있게 도와주는 추상 레이어를 제공한다.  
생성된 프록시는 스프링 빈으로 등록돼야 한다. 스프링은 프록시 오브젝트를 생성해주는 기술을 추상화한 팩토리 빈을 제공해준다.  

스프링의 ProxyFactoryBean은 프록시를 생성해서 빈 오브젝트로 등록하게 해주는 팩토리 빈이다. 기존에 만들었던 TxProxyFactoryBean과 달리, ProxyFactoryBean은  
순수하게 프록시를 생성하는 작업만을 담당하고 프록시를 통해 제공해줄 부가기능은 별도의 빈에 둘 수 있다.  

ProxyFactoryBean이 생성하는 프록시에서 사용할 부가기능은 MethodInterceptor 인터페이스를 구현해서 만든다. MethodInterceptor는 InvocationHandler와 비슷하지만  
한 가지 다른 점이 있다. InvocationHandler의 invoke() 메소드는 타깃 오브젝트에 대한 정보를 제공하지 않는다. 따라서 타깃은 InvocationHandler를 구현한 클래스가 직접 알고 있어야 한다.  
반면에 MethodInterceptor의 invoke()메소드는 ProxyFactoryBean으로부터 타깃 오브젝트에 대한 정보까지도 함께 제공받는다. 그 차이 덕분에 MethodInterceptor는  
타깃 오브젝트에 상관없이 독립적으로 만들어질 수 있다. 따라서 MehtodInterceptor 오브젝트는 타깃이 다른 여러 프록시에서 함께 사용할 수 있고, 싱글톤 빈으로 등록 가능하다.

```java
import java.util.Locale;

public class DynamicProxyTest {

    @Test
    public void proxyFactoryBean() {
        ProxyFactoryBean pfBean = new ProxyFactoryBean();
        pfBean.setTarget(new HelloTarget()); // 타깃설정
        pfBean.addAdvice(new UppercaseAdvice()); // 부가기능을 담은 어드바이스를 추가한다. 여러개 추가할 수도 있다.

        Hello proxiedHello = (Hello) pfBean.getObject(); // FactoryBean이므로 getObject()로 생성된 프록시를 가져온다.
        ...
    }

    static class UppercaseAdvice implements MethodInterceptor {

        public Object invoke(MethodInvocation invocation) throws Throwable {
            // 리플렉션의 Method와 달리 메소드 실행 시 타깃 오브젝트를 전달할 필요가 없다. MethodInvocation은 메소드 정보와 함께 타깃 오브젝트를 알고 있기 때문이다.
            String ret = (String) invocation.proceed();
            return ret.toUpperCase();
        }
    }
}
```

#### 어드바이스: 타깃이 필요 없는 순수한 부가기능
InvocationHandler를 구현했을 때와 달리 MethodInterceptor를 구현한 UppercaseAdvice에는 타깃 오브젝트가 등장하지 않는다. MethodInterceptor로는 메소드 정보와 함께  
타깃 오브젝트가 담긴 MethodInvocation 오브젝트가 전달된다. MethodInvocation은 타깃 오브젝트의 메소드를 실행할 수 있는 기능이 있기 때문에 MethodInterceptor는  
부가기능을 제공하는 데만 집중할 수 있다.  

MethodInvocation은 일종의 콜백 오브젝트로, proceed() 메소드를 실행하면 타깃 오브젝트의 메소드를 내부적으로 실행해주는 기능이 있다. MethodInvocation 구현 클래스는  
일종의 공유 가능한 템플릿처럼 동작하는 것이다. 바로 이 점이 JDK의 다이내믹 프록시를 직접 사용하는 코드와 스프링이 제공해주는 프록시 추상화 기능인 ProxyFactoryBean을 사용하는 코드의  
가장 큰 차이점이자 ProxyFactoryBean의 장점이다. ProxyFactoryBean은 작은 단위의 템플릿/콜백 구조를 응용해서 적용했기 때문에 템플릿 역할을 하는 MethodInvocation을  
싱글톤으로 두고 공유할 수 있다.  

advice를 추가할 때 수정자가 아닌 addAdvice()라는 메소를 사용하는데 ProxyFactoryBean에는 여러 개의 MethodInterceptor를 추가할 수 있다.  
ProxyFactoryBean 하나만으로 여러 개의 부가 기능을 제공해주는 프록시를 만들 수 있다는 뜻이다. 아무리 많은 부가기능을 적용하더라도 ProxyFactoryBean 하나로 충분하다.  

그런데 MethodInterceptor 오브젝트를 추가하는 메소드 이름은 addMethodInterceptor가 아니라 addAdvice다. MethodInterceptor는 Advice 인터페이스를 상속하고 있는  
서브인터페이스이기 때문이다. 이름에서 알 수 있듯이 MethodInterceptor처럼 타깃 오브젝트에 적용하는 부가기능을 담은 오브젝트를 스프링에서는 어드바이스라고 부른다.  

마지막으로 ProxyFactoryBean을 적용한 코드에는 프록시가 구현해야 하는 Hello라는 인터페이스를 제공해주는 부분이 없다. setInterface() 메소드를 통해서 구현해야할 인터페이스를 지정할 수 있다.  
하지만 인터페이스를 굳이 알려주지 않아도 ProxyFactoryBean에 있는 인터페이스 자동검출 기능을 사용해 타깃 오브젝트가 구현하고 있는 인터페이스 정보를 알아낸다. 
어드바이스는 타깃 오브젝트에 종속되지 않는 순수한 부가기능을 담은 오브젝트다.

#### 포인트컷: 부가기능 적용 대상 메소드 선정 방법
기존에 InvocationHandler를 직접 구현했을 때는 부가기능 적용 외에도 한 가지 작업이 더 있었다. 메소드의 이름을 가지고 부가기능을 적용 대상 메소드를 선정하는 것이었다.  
MethodInvocation 오브젝트는 여러 프록시가 공유해서 사용할 수 있다. 그 덕분에 싱글톤으로 등록할 수 있었다.  
그래서 여러 오브젝트가 공유하는 MethodInvocation에 특정 프록시만 적용되는 패턴을 넣으면 문제가 된다.  

MethodInterceptor에는 재사용 가능한 순수한 부가기능 제공 코드만 남겨두고 프록시에 부가기능 적용 메소드를 선택하는 기능을 넣자.  

기존의 InvocationHandler는 타깃과 메소드 선정 알고리즘에 의존하고 있다. 한번 빈으로 구성된 InvocationHandle 오브젝트는 오브젝트 차원에서 특정 타깃을 위한 프록시에 제한된다.  
그래서 InvocationHandler는 굳이 따로 빈으로 등록하는 대신 TxProxyFactoryBean 내부에서 매번 생성하였다. 결국 OCP 원칙을 깔끔하게 지키지 못하였다.  

반면에 스프링의 ProxyFactoryBean 방식은 두 가지 확장 기능인 부가기능과 메소드 선정 알고리즘을 활용하는 유연한 구조를 제공한다.  
스프링은 부가기능을 제공하는 오브젝트를 어드바이스라고 부르고, 메소드 선정 알고리즘을 담은 오브젝트를 포인트컷이라고 부른다.  
어드바이스와 포인트컷은 모두 프록시에 DI로 주입돼서 사용된다. 두 가지 모두 여러 프록시에서 공유가 가능하도록 만들어지기 때문에 스프링의 싱글톤 빈으로 등록 가능하다.  

프록시는 클라이언트로부터 요청을 받으면 먼저 포인트컷에게 부가기능을 부여할 메소드인지를 확인해달라고 요청한다. 포인트컷은 PointCut 인터페이스를 구현해서 만들면 된다.  
프록시는 포인트컷으로부터 부가기능을 적용할 대상 메소드인지 확인받으면, MethodInterceptor 타입의 어드바이스를 호출한다. 어드바이스는 JDK의 다이내믹 프록시의 InvocationHandler와 달리  
직접 타깃을 호출하지 않는다. 자신이 공유돼야하므로 타깃 정보라는 상태를 가질 수 없다. 따라서 타깃에 직접 의존하지 않도록 일종이 템플릿 구조로 설계되어 있다.  
어드바이스가 부가기능을 부여하는 중에 타깃 메소드의 호출이 필요하면 프록시로부터 전달받은 MethodInvocation 타입 콜백 오브젝트의 proceed() 메소드를 호출해주기만 하면 된다.  

```java
@Test
public void pointcutAdvisor() {
    ProxyFactoryBean pfBean = new ProxyFactoryBean();
    pfBean.setTarget(new HelloTarget());
    
    NameMatchMethodPointCut pointcut = new NameMatchMethodPointcut(); // 메소드 이름을 비교해서 대상을 선정하는 알고리즘을 제공하는 포인트컷 생성
    pointcut.setMappedName("sayH*"); // 이름 비교조건 설정
    
    // 포인트컷과 어드바이스 한번에 추가
    pfBean.addAdvisor(new DefaultPointcutAdvisor(pointcut, new UppercaseAdvice()));
}
```

포인트컷이 필요 없을 때는 ProxyFactorBean의 addAdvice() 메소드를 호출해서 어드바이스만 등록하면 됐다. 그런데 포인트컷을 함께 등록할 때는 어드바이스와 포인트컷을  
Advisor 타입으로 묶어서 addAdvisor() 메소드를 호출해야 한다. 별개의 오브젝트가 아니라 하나의 오브젝트로 호출하는 이유는 여러 개의 어드바이스와 포인트컷이 추가될 수 있기 때문이다.  
어떤 어드바이스(부가기능)에 대해 어떤 포인트컷(메소드 선정)을 적용할지 애매해진다. 이렇게 어드바이스와 포인트컷을 묶은 오브젝트를 인터페이스 이름을 따서 어드바이저라고 부른다.

어드바이저 = 포인트컷(메소드 선정 알고리즘) + 어드바이스(부가기능)


