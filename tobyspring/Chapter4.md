# 4장 예외

## 4.1 사라진 SQLException

### 요약
3장에서는 deleteAll() 메소드에서는 SQLException을 throw한다. 하지만 jdbcTemplate으로 바꾼 뒤에는 throw가 사라졌다.  

초난감 예외처리의 대표들을 알아보자.  

#### 예외 블랙홀
catch 에서 예외를 잡고 아무것도 하지 않는 것이다. 예외를 처리할 때 반드시 지켜야 할 핵심 원칙은 한가지이다. 모든 예외는 적절하게 복구되든지 아니면  
작업을 중단시키고 운영자 또는 개발자가에게 분명하게 통보돼야 한다.  

#### 무의미하고 무책임한 throws
chtch 블록으로 예외를 잡아봐야 해결할 방법도 없고 JDK API나 라이브러리가 던지는 각종 이름도 긴 예외들을 처리하는 코드를 매번 throws로 선언하기도 귀찮아지기 시작하면  
메소드 선언에 throws Exception을 기계적으로 무책임하게 던진다.  

자바에서 예외는 크게 3가지가 있다.  
- Error : 에러는 시스템에 뭔가 비정상적인 상황이 발생했을 경우에 사용된다. 주로 자바 VM에서 발생시키는 것이고 어플리케이션 코드에서 잡으려고 하면 안된다.  
- Exception과 체크 예외 : 치크 예외는 Exception 클래스의 서브클래스이면서 RuntimeException 클래스를 상속하지 않은 것들이고, 후자는 RuntimeException을  
상속한 클래스들을 말한다. 체크 예외가 발생할 수 있는 메소드를 사용할 경우 반드시 예외를 처리하는 코드를 함께 작성해야 한다.
- RuntimeException과 언체크/런타임 예외 : RuntimeException 클래스를 상속한 예외들은 명시적인 예외처리를 강제하지 않기 때문에 언체크 예외라고 불린다.  
런타임 예외는 주로 프로그램의 오류가 있을 때 발생하도록 의도된 것들이다.
  
예외 처리하는 일반적인 방법

- 예외 복구 : 예외 상황을 파악하고 문제를 해결해서 정상 상태로 돌려놓는 것이다.  
- 예외 처리 회피 : 예외 처리를 자신이 담당하지 않고 자신을 호출한 쪽으로 던져버리는 것이다.  
- 예외 전환 : 예외를 복구해서 정상 상태로 만들 수 없기 때문에 예외를 메소드 밖으로 던진다. 하지만 예외 회피와 달리 발생한 예외를 그대로 넘기는 게 아니라 적절한  
예외로 전환해서 던진다는 특징이 있다. 예외 전환은 보통 두 가지 목적으로 사용된다. 첫째는 내부에서 발생한 예외를 그대로 던지는 것이 그 예외 상황에 대해 적절한 의미를  
  부여해주지 못하는 경우 의미를 분명하게 해주는 예외로 변경하는 것이다. 보통 전환하는 예외에 원래 발생한 예외를 담아서 중첩 예외로 만드는 것이 좋다.  
  중첩 예외는 getCause() 메소드를 이용해서 처음 발생한 예외가 무엇인지 확인할 수 있다. 두번째는 예외를 처리하기 쉽고 단순하게 만들기 위해 포장하는 것이다.  
  주로 체크 예외를 언체크 예외로 변환한다.  
  어차피 복구하지 못할 예외라면 어플리케이션 코드에서는 런타임 예외로 포장해서 던져버리고, 예외 처리 서비스 등을 이용해 자세한 로그를 남기고, 메일로 통보를 하는게 좋다.  
  
대응이 불가능한 체크 예외라면 빨리 런타임 예외로 전환해서 던지는게 낫다. 최근 등장하는 표준 스펙에서는 체크 예외 대신에 언체크 예외로 정의하는 것이 일반화되고 있다.  

## 4.2 예외 전환

### 요약 
스프링의 JdbcTemplate이 던지는 DataAccessException은 런타임 예외로 SQLException을 포장해주는 역할을 한다.  

#### Jdbc의 한계
첫번째는 비표준 SQL이다. 특혈한 기능을 제공하는 함수를 SQL에 사용하려면 대부분 비표준 SQL 문장이 만들어진다. 이러한 비표준 SQL는 DAO에 종속적이게 되고  
다른 DB로 변경하려면 많은 코드를 수정해야한다.
두번째는 SQLException이다. DB마다 에러의 종류와 원인이 제각각이기 때문에 JDBC는 데이터 처리중에 발생한 다양한 예외를 SQLException 하나에 담아 버린다.  
그런데 SQLException의 getErrorCode()로 가져올 수 있는 DB 에러 코드는 DB 마다 모두 다르다.

```java
  if (e.getErrorCode() == MysqlErrorNumbers.ER_DUP_ENT) {...}
```

기존에 사용한 errorCode를 사용해 작성한 코드는 MySql에서만 쓰이는 코드이므로 DB가 변경된다면 코드를 모두 고쳐야 한다.  

DB 종류가 바뀌더라도 DAO를 수정하지 않으려면 위의 두가지 문제를 해결해야 한다. 여기서는 SQLException의 비표준 에러코드와 SQL 상태 정보에 대한 해결책을 알아보자.  

SQL 상태 코드는 JDBC 드라이버를 만들 때 들어가는 것으로 달라질 수 있지만 DB 에러코드는 비교적 일관성이 유지된다.  
스프링에서는 DB별 에러 코드를 분류해서 스프링이 정의한 예외 클래스와 매핑해놓은 에러 코드 매핑 정보 테이블을 만들어두고 이를 이용한다.  

JdbcTemplate은 체크 예외인 SQLException을 런타임 예외인 DataAccessException으로 포장을 해준다. 그리고 DB의 종류와 상관없이 중복키로 발생하는 에러는  
DataAccessException의 서브클래스인 DuplicateKeyException으로 매핑돼서 던져진다.

DataAccessException은 JDBC의 SQLException을 전환하는 용도로만 만들어진 건 아니다. JDBC 외의 자바 데이터 엑세스 기술에서 발생하는 예외에도 적용된다.  
JPA와 같은 퍼시스턴스 기술에도 사용되는데 의미가 같은 예외라면 데이터 엑게스 기술의 종류와 상관없이 일관된 예외가 발생하도록 만들어 준다.  
데이터 엑세스 기술에 독립적인 추상화된 예외를 제공하는 것이다.

DI를 통해 주입받은 인터페이스를 실행할 때 인터페이스에 throws가 붙어 있지 않으면 사용하는 곳에서 예외 처리를 할 수 없다.  
데이터 엑세스 기술은 각각 다른 예외를 던지기 때문에 interface 추상 메소드에 throws를 추가할 경우 각 기술에 종속적이 된다.  
따라서 체크 예외를 런타임 예외로 포장을 하면 throws가 없어지고 종속성이 없어진다. 하지만 데이터 엑세스 기술이 항상 복구 불가능한 것은 아니기 때문에  
어플리케이션에서 사용하지 않더라도 시스템 레벨에서 데이터 엑세스 예외를 의미 있게 분류할 필요도 있다.  

그래서 스프링을 자바의 다양한 엑세스 기술을 사용할 때 발생하는 예외들을 추가상화해서 DataAccessException 계층 구조 안에 정리해놓았다.  
JdbcTemplate과 같이 스프링의 데이터 엑세스 지원 기술을 이용해 DAO를 만들면 사용 기술에 독립적인 일관성 있는 예외를 던질 수 있다.  
결국 인터페이스 사용, 런타임 예외 전환과 함께 DataAccessException 예외 추상화를 적용하면 데이터 엑세스 기술과 구현 방법에 독립적인 이상적인 DAO를 만들 수 있다.  

#### 기술에 독립적인 UserDao 만들기
지금까지 만들어서 써왔던 UserDao 클래스를 이제 인터페이스과 구현으로 분리해보자. 인터페이스를 분리할 때는 앞에 I라는 접두어를 붙이는 방법도 있고,  
인터페이스 이름은 가장 단순하게 구현하고 구현 클래스는 각각의 특징을 따르는 이름을 붙이는 경우도 있다.  
setDataSource() 메소드는 추상메소드로 추가할 필요가 없다. UserDao의 구현 방법에 따라 변경될 수도 있고, UserDao를 사용하는 클라이언트가 알고 있을 필요도 없다.  

테스트 코드에서 @Autowired를 통해 UserDao type으로 받는 것을 UserDaoJdbc로 변경할 필요는 없다. 구현 기술에 상관 없이 DAO의 기능 동작에만 관심이 있기 때문이다.  

테스트 코드에서 에외가 발생하는 경우 @Test(expected=DataAccessException.class)를 통해 확인할 수 있다.  

#### DataAccessException 활용 시 주의 사항
DuplicateKeyException 같은 경우 JDBC를 이용하는 경우에만 발생한다. 에러코드와 달리 이러한 예외들은 세분화되어 있지 않기 때문이다.  
학습테스트를 만들어서 구현 기술에 대한 예외를 확인하는 것이 좋다.

