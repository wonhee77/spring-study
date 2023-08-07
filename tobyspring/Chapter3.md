# 3장 템플릿

## 3.1 다시 보는 초난감 DAO

### 요약
DB 커넥션이라는 제한된 리소스를 공유해 사용하는 서버에서 동작하는 JDBC 코드에는 예외처리를 반드시 해야된다.  
예외가 발생 했을 때 반드시 리소스를 반납해야 한다.  

try/catch/finally 를 사용하여 예외가 발생 시 connection을 close()해주는 코드를 추가한다.  


## 3.2 변하는 것과 변하지 않는 것

### 요약
try/catch/finally를 적용했지만 2중으로 중첩이 되는데다 모든 코드에 반복돼서 나타난다.  
또한 실수로 어딘가에서 close() 하는 메소드를 빼먹는다면 리소스가 반납되지 않아 커넥션 풀이 가득 차버리게 된다. 이는 알아채기 힘들다.  

분리와 재사용을 위해 디자인 패턴을 적용해보자. 코드 상에서는 ps = c.prepareStatement("delete from users"); 부분을 제외한 나머지 커넥션을 가져오고,  
실행하며 예외처리를 하는 부분은 모두 공통적인 부분이다. 변하지 않는 부분이 더 많기 때문에 변하는 부분을 makeStatement(Connection c) 함수로 분리한다.  

#### 템플릿 메소드 패턴의 적용
다음으로 템플릿 메소드 패턴을 통해 분리한다. 템플릿 메소드 패턴은 상속을 통해 기능을 분리한다. 변하지 않는 부분은 슈퍼클래스에 두고 변하는 부분은 추상 메소드로  
정의해둬서 서브클래스에서 오버라이드하여 새롭게 정의해 쓰도록 하는 것이다.  

템플릿 메소드 패턴을 적용하면 OCP가 잘 지켜지기는 하지만 매번 새로운 클래스가 생성되어야하기 때문에 제한이 많다.  
또 확장구조가 설계시에 고정되어 버린다. 컴파일 시점에서 이미 관계들이 결정 된다.  

#### 전략 패턴의 적용 
OCP를 잘 지키는 구조이면서도 템플릿 메소드 패턴보다 유연하고 확장성이 뛰어난 것이 오브젝트를 아예 둘로 분리하고 클래스 레벨에서는 인터페이스를 통해서만 의존하도록 만드는  
전략패턴이다. 전략패턴은 확장에 대하여 변하는 부분을 별도의 클래스로 만들어 추상화된 인터페이스를 통해 위임하는 방식이다.  

makePreparedStatement 메소드를 가진 StatementStrategy 인터페이스를 만들고 DeleteAllStatement 구현체를 만들어 컨텍스트에서 이 구현체를 생성하면  
전략패턴을 구현할 수 있다.
하지만 context 내부에서 객체를 직접 생성하므로 완벽한 OCP로 볼 수는 없다.  

컨텍스트에 해당하는 부분을 별도의 메소드로 분리하고 변수로 StatementStrategy로 받도록 변경하자. 기존의 deleteAll() 메서드가 클라이언트가 되어  
컨텍스트로 분리된 메소드에 StatementStrategy를 주입해주는 역할을 하게 된다.  

```java
    public vlid deleteAll() throws SQLExceptoin {
        StatementStrategy st = new DeleteAllStatemnet();
        jdbcContextWithStatementStrategy(st);
    }
```


## 3.3 JDBC 전략 패턴의 최적화

### 요약
UserDao의 add() 메소드에는 User라는 부가정보가 필요하다. 따라서 아래와 같이 AddStatement의 생성자로 User 객체를 받도록 변경한다.
```java
    public class AddStatement implements StatemetStrategy {
        User user;
        public AddStatement(User user) {
            this.user = user;
        }
    }
```

템플릿 메소드 패턴에서 전략패턴으로 넘어오면서 훨씬 더 깔끔한 코드가 만들어졌지만 지금도 여전히 statementStrategy 구현체 클래스 파일들이 계속 늘어나는 구조이다.

#### 로컬 클래스
파일이 많아지는 것은 UserDao에서 밖에 사용되지 않기 때문에 UserDao 클래스에 내부클래스로 클래스를 만들어버릴 수 있다.  
AddStatement 클래스를 add() 메소드 내부에 집어넣으면 AddStatement 클래스는 add() 메소드 내의 변수에 바로 접근할 수 있다. 이때 내부 클래스에서  
외부의 변수를 사용할 때는 외부 변수는 반드시 final로 선언해야 한다.

#### 익명 내부 클래스
AddStatement 클래스는 한 번만 사용될 것이기 때문에 굳이 클래스로 선언할 필요 없이 익명클래스로 만들고 jdbcContextWithStatementStrategy 함수에 변수로 직접 전달할 수 있다.

### 용어
`익명 내부 클래스` : 이름을 갖지 않는 클래스로 재사용할 필요가 없고, 구현한 인터페이스 타입으로만 사용할 경우에 유용하다.   
내부 클래스에서 외부 변수에 접근하려면 외부 변수는 final이거나 effectively final이어야 한다.  
외부 변수는 메소드 영역이기 때문에 할일이 끝나면 데이터가 스택에서 다 지워진다. 그래서 값이 변한다거나 할 경우 나중에 실행될 클래스에서 동기화 문제가 생길 수 있다.  
new 인터페이스이름() { 클래스 본문 };


## 3.4 컨텍스트와 DI

### 요약
전략패턴의 구조로 보자면 UserDao가 클라이언트이고 익명 내부 클래스가 개별 전략, JdbcContextWithStatementStrategy() 메소드가 컨텍스트이다.  
JdbcContextWithStatementStrategy()는 다른 Dao에서도 사용할 수 있기 때문에 클래스로 독립시키자.

JdbcContext라는 이름의 클래스를 만들고 DataSource 타입빈을 DI로 받을 수 있도록 수정자를 만든다. 그리고 함수의 이름도 workWithStatementStrategy로 변경하였다.  
UserDao에서는 JdbcContext를 DI로 받을 수 있도록 수정자를 만들고 add() 와 deleteAll()함수 내부도 jdbcContext.workWithStatementStrategy로 변경한다.

#### 스프링 빈으로 DI
UserDao에서 JdbcContext는 인터페이스가 아닌 구현체로 DI를 하고 있다.  
인터페이스를 사용해서 클래스를 자유롭게 변경할 수 있게 하지는 않았지만, JdbcContext를 UserDao와 DI 구조로 만들어야할 이유를 생각해보자.

첫번째, JdbcContext가 스프링 컨테이너의 싱글톤 레지스트리로 관리되는 싱글톤 빈이기 때문이다. JdbcContext는 읽기 전용으로 여러 곳에서 사용해도 문제가 없다.  
매번 새로 생성하지 않아도 된다.  
두번째, JdbcContext가 DI를 통해 다른 빈에 의존하고 있기 때문이다. 두번째 이유가 중요하다. JdbcContext는 DataSource 오브젝트를 주입받고 있다.  
DI를 위해서는 주입되는 오브젝트와 주입받는 오브젝트 양쪽 모두 스프링 빈으로 등록되어야 한다.

JdbcContext는 테스트에서도 바뀔 이유가 없기 때문에 강한 결합을 가진 관계를 허용하면서 싱글톤과 JdbcContext에 대한 DI 필요성을 위해 스프링의 빈으로 등록해서  
UserDao에 DI 되도록 만들어도 좋다.

#### 코드를 이용하는 수동 DI
UserDao 내부에서 직접 DI를 적용할 수 있다. 이 방법을 사용하면 싱글톤으로 만드는 것은 포기해야 된다.  
수동 DI를 사용하면 위에서 본 두번째 이유를 신경써야 한다. DataSource는 스프링에서 관리하는 빈이기 때문에 주입을 할 수 없기 때문이다.  
이런 경우는 UserDao에서 DI를 맡길 수 있다. UserDao는 DataSource를 직접 필요하지는 않지만 JdbcContext에게 전달하는 용도로만 사용하는 것이다.
```java
public class UserDao {
    private JdbcContext jdbcContext;

    public void setDataSource(DataSource dataSource) {
        this.jdbcContext = new JdbcContext();
        this.jdbcContext.setDataSource(dataSource);
        this.dataSource = dataSource; // 아직 JdbcContext를 적요하지 않는 메소드를 위해 저장해준다.
    }
}
```

JdbcContext와 같이 인터페이스를 사용하지 않고 Dao와 밀접한 관계를 갖는 클래스를 DI에 적용하는 방법 두 가지를 알아봤다.  
스프링 DI를 하는 방법은 실제 의존관계가 설정 파일에 잘드러나지만 구체적인 관계가 설정에 직접 노출되는 단점이 존재한다.  
코드를 통해 DI를 하는 방법은 필요에 따라 내부에서 DI를 은밀히 수행할 수 있지만, 싱글톤을 만들 수 없고, DI를 위한 부가적인 코드가 필요하다.  
나은 방법은 없지만 상황에 따라서 잘 선택을 해야한다.



