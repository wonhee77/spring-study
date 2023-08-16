# 5장 서비스의 추상화

## 5.1 사용자 레벨 관리 기능 추가

### 요약
지금까지 만들었던 UserDao는 CRUD의 기초 작업만 가능하다. 지금까지 만들었던 UserDao를 다수의 회원이 가입할 수 있는 인터넷 서비스의 사용자 관리 모듈에 적용한다고 생각해보자.  
사용자 관리 기능에는 단지 정보를 넣고 검색하는 것 외에도 정기적으로 사용자의 활동 내역을 참고해서 레벨을 조정해주는 기능이 필요하다.  
- 사용자의 레벨은 BASIC, SILVER, GOLD 세 가지 중 하나다.  
- 사용자가 처음 가입하면 BASIC 레벨이고 활동에 따라서 한 단계식 업그레이드될 수 있다.  
- 가입 후 50회 이상 로그인하면 BASIC에서 SILVER 레벨이 된다.  
- SILVER 레벨이면서 30번 이상 추천을 받으면 GOLD 레벨이 된다.  
- 사용자 레벨의 변경 작업은 일정한 주기를 가지고 일괄적으로 진행된다. 변경 작업 전에는 조건이 만족하더라도 레벨의 변경이 일어나지 않는다.  

User 클래스에 사용자의 레벨을 저장할 필드를 추가하자. 문자나 숫자 타입은 안정성이 떨어지기 때문에 enum을 이용하자.  
```java
public enum Level {
    BASIC(1), SILVER(2), GOLD(3);
    
    private final int value;
    
    Level(int value) {
        this.value = value;
    }
    
    public int intValue() {
        return value;
    }
    
    public static Level valueOf(int value) {
        switch(value) {
            case 1: return BASIC;
            case 2: return SILVER;
            case 3: return GOLD;
            default: throw new AssertionError("Unknown value: " + value);
        }
    }
}
```

User 필드 추가

```java
public class User {
    ...
    Level level;
    int login;
    int recommend;

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }
}
```

#### UserDaoTest 테스트 수정
기존의 코드에 새로운 기능을 추가하려면 테스트를 먼저 만드는 것이 안전하다.

UserDaoJdbc의 UserMapper에 새롭게 추가된 필드를 추가한다.  
UserDao 인터페이스에 update(User user) 추상 메소드를 추가한다.  
UserDaoJdbc의 update() 메소드는 add()와 비슷한 방식으로 만들면 된다.  

비즈니스 로직을 처리할 UserService를 추가한다. 그리고 인터페이스 타입으로 userDao 빈을 DI 받아 사용하게 만든다.

#### UpgradeLevels() 메소드

```java
    public void upgradeLevels() {
        List<User> users = userDato.getAll();
        for(User user : users) {
            Boolean changed = null;
            if (user.getLevel() == Level.BASIC && user.getLogin() >= 50){
                user.setLevel(Level.SILVER);
                chagned = true;
            }
            else if (user.getLevel() == Level.SILVER && user.getRecommnend >= 30){
                user.setLevel(Level.GOLD);
                changed = true;
            }
            else if (user.getLevel() == Level.GOLD) { changed = false;}
            else { changed = false; }
            
            if (chagned) { userDao.update(user); }
        }
    }
```

User를 추가하기 위한 로직은 비즈니스 로직이기 때문에 UserService에서 작성한다.

```java
    public void add(User user) {
        if (user.getLevel() == null) user.setLevel(Level.BASIC);
        userDao.add(user);
    }
```

#### upgradeLevels() 메소드 코드의 문제점

```java
    if (user.getLevel() [1] == Level.BASIC && user.getLogin() >= 50 [2]) {
        user.setLevel(Level.SILVER); [3]
        chagned = true; [4]
    }
    
    if (changed) { userDao.update(user);} [5]
```
[1] 현재 레벨을 파악하는 로직, [2] 업그레이드 조건을 담은 로직, [3] 다음 단계의 레벨이 무엇이며 업그레이드를 위한 작업은 어떤 것인지가 함께 담겨있다.  
[4] 그 자체로는 의미없고 단지 멀리 떨어져 있는 [5]의 작업이 필요한지를 알려주기 위해 임시 플래그를 설정해주는 것이다.  
관련이 있어 보이지만 사실 성격이 조금씩 다른 것들이 섞여 있거나 분리돼서 나타나는 구조다.  

#### upgradeLevel() 리팩토링

```java
    public void upgradeLevels() {
        List<User> users = userDato.getAll();
        for (User user : users) {
            if (canUpgradeLevel(user)) {
                upgradeLevel(user);       
            }
        }
    }
```

```java
    private boolean canUpgradeLevel(User user) {
        Level currentLevel = user.getLevel();
        switch(currentLevel) {
            case BASIC: return (user.getLogin() >= 50);
            case SILVER: return (user.getRecommend() >= 30);
            case GOLD: return false;
            default: throw new IllegalArgumentException("Unkown Level: " + currentLevel);
        }
    }
```

```java
    private void upgradeLevel(User user) {
        if (user.getLevel() == Level.BASIC) user.setLevel(Level.SILVER);
        else if (user.getLevel() == Level.SILVER) user.setLEvel(Level.GOLD);
        userDao.update(user);
    }
```

레벨이 많아진다면 if 문도 계속 추가될 것이다. 레벨의 순서와 다음 레벨이 무엇인지 결정하는 일은 Level에게 맡기자.  
```java
public enum Level {
    GOLD(3, null), SILVER(2, GOLD), BASIC(3, SILVER);
    
    private final int value;
    private final Level next;
    
    public Level nextLevel() {
        return this.next;
    }
}
```

레벨을 업그레이드하는 로직을 UserService에서 User로 옮긴다. 
```java
    public void upgradeLevel() {
        Level nextLevel = this.level.getNext();
        if (nextLevel == null) {
            throw new IllegalStateException(this.level + "은 업그레이드가 불가능합니다.")
        } else {
            this.level = nextLevel;    
        }   
    }
```

## 5.2 트랜잭션 서비스 추상화

### 요약
정기 사용자 레벨 관리 작업을 수행하는 도중에 네트워크가 끊기거나 서버에 장애가 생겨서 작업을 완료할 수 없다면 작업을 돌려놓아야 한다.  

테스트 코드에서 중간에 에러를 발생시키려고 실제 코드를 변경할 수는 없다. 테스트에서는 UserService의 대역을 만들어 해결하자.  
먼저 테스트용으로 UserService를 상속한 클래스를 하나 만든다. 테스트에서만 사용할거면 번거롭게 파일을 만들지 말고 내부에 스태틱 클래스를 만드는 것이 간편하다.  
UserService의 대부분은 private 메소드이므로 상속을 할 수 없다. upgradeLevel() 메소드는 public 메소드라 상속이 가능하다.  
이 메소드에 전달되는 User 오브젝트를 확인해서 네 번째 User 오브젝트가 전달됐을 경우에 강제로 예외를 발생시키려고 한다.  

예외를 발생시킬 User의 Id를 변수로 받을 수 있도록 메소드를 만들고, 해당 upgradeLevel() 메소드가 해당 id를 받으면 exception을 throw하게 한다.  

테스트 코드에서 TestUserSerivce를 생성하고 생성자 파라미터로 예외를 발생시킬 id를 넣어준다. 컨테이너에 종속적이지 않은 평범한 자바 코드로 만들어지는 스프링 DI의  
장점이 바로 이런 것이다. 스프링의 도움을 받지 않고 DI가 가능하다.  

4번째 id에서 예외가 발생하였을 경우 이전의 user의 level을 확인해보면 중간에 예외가 발생하였음에도 불구하고 변경된 것이 그래도 반영되어 있다.  
모든 사용자의 레벨을 업그레이드하는 작업인 upgradeLevels() 메소드가 하나의 트랜잭션 안에서 동작하지 않았기 때문에 그렇다.  

#### 트랜잭션의 경계 설정
DB는 그 자체로 완벽한 트랜잭션을 지원한다. 하나의 SQL 명령을 처리하는 경우는 DB가 트랜잭션을 보장해준다고 믿을 수 있다.  
하지만 여러개의 SQL이 사용되는 작업을 하나의 트랜잭션으로 취급해야 하는 경우도 있다. 두 개의 SQL 중 첫번째 SQL에서 실패를 한다면 결과를 되돌려야 하고,  
이런 취소 작업을 트랜잭션 롤백이라고 한다. 반대로 여러 개의 SQL을 하나의 트랜잭션으로 처리하는 경우에 모든 SQL 수행 작업이 다 성공적으로 마무리됐다고 DB에 알려줘서  
작업을 확정시켜야 한다. 이것을 트랜잭션 커밋이라고 한다.  

모든 트랜잭션은 시작하는 지점과 끝나는 지점이 있다. 시작하는 방법은 한가지이지만 끝나는 방법은 두 가지이다. 모든 작업을 무효화하는 롤백과 모든 작업을 다 확정하는 커밋이다.  
어플리케이션 내에서 트랜잭션이 시작되고 끝나는 위치를 트랜잭션의 경계라고 부른다. 

```java
Connection c = datasource.getConnection();

c.setAutoCommit(false); // 트랜잭션 경계의 시작
try {
        PreparedStatement st1 = c.prepareStatement("update users ...");
        st1.executeUpdate();
    
        PreparedStatement st2 = c.prepareStatement("delete users ...");
        st2.executeUpdate();
    
        c.commit(); // 트랜잭션 커밋
    } catch(Exception e) {
        c.rollback(); // 트랜잭션 롤백
    }
c.close()
```

JDBC의 트랜잭션은 하나의 Connection을 가져와 사용하다가 닫는 사이에서 일어난다. 트랜잭션의 시작과 종료는 Connection 오브젝트를 통해 이뤄지기 때문이다.  
JDBC에서 트랜잭션을 시작하려면 자동커밋 옵션을 false로 만들어주면 된다. 트랜잭션이 한 번 시작되면 commit() 또는 rollback() 메소드가 호출될 때까지의 작업이  
하나의 트랜잭션으로 묶인다.

기존 upgradeLevels()에는 코드 어디에도 트랜잭션을 시작하고, 커밋하고, 롤백하는 트랜잭션 경계설정 코드가 존재하지 않는다.  
일반적으로 트랜잭션은 커넥션보다도 존재 범위가 짧다. 따라서 템플릿 메소드가 호출될 때마다 트랜잭션이 새로 만들어지고 메소드를 빠져나오기 전에 종료된다.  
결국 JdbcTemplated의 메소드를 사용하는 UserDao는 각 메소드마다 하나씩의 독립적인 트랜잭션으로 실행될 수 밖에 없다.  

데이터 엑세스 코드를 DAO로 만드어서 분리해놓았을 경우에는 DAO 메소드를 호출할 때마다 하나의 새로운 트랜잭션이 만드어지는 구조가 될 수 밖에 없다.  
DAO 메소드에서 DB 커넥션을 매번 만들기 때문에 어쩔 수 없이 나타나는 결과다. 결국 DAO를 사용하면 비즈니스 로직을 담고 있는 UserService 내에서 진행되는  
여러가지 작업을 하나의 트랜잭션으로 묶는 일이 불가능해진다.

일련의 작업이 하나의 트랜잭션으로 묶이려면 그 작업이 진행되는 동안 DB 커넥션도 하나만 사용돼야 한다.

#### 비즈니스 로직 내의 트랜잭션 경계설정
UserService의 upgradeLevels() 메소드 내에서 트랜잭션의 경계를 설정해주어야 한다. 그래서 메소드 내에 connection이 필요한데 여기서 생성된 Connection 오브젝트를  
가지고 데이터 엑세스 작업을 진행하는 코드는 UserDao의 update() 메소드 안에 있어야 한다. 트랜잭션 때문에 DB 커넥션과 트랜잭션 관련 코드는 어쩔 수 없이 UserService로 가져왔지만  
순수한 데이터 엑세스 로직은 UserDao에 둬야 하기 때문이다. UserDao의 update() 메소드는 반드시 upgradeLevels() 메소드에서 만든 Connection을 사용해야 한다.  

#### UserService 트랜잭션 경계설정의 문제점
UserService와 UserDao를 Connection 오브젝트를 넘기는 식으로 수정하면 트랜잭션 문제는 해결할 수 있겠지만, 그 대신 여러가지 새로운 문제가 발생한다.  
첫째는 DB 커넥션을 비롯한 리소스의 깔끔한 처리를 가능하게 했던 JdbcTemplate을 더 이상 활용할 수 없다.  
두번째는 DAO의 메소드와 비즈니스 로직을 담고 있는 UserService의 메소드에 Connection 파라미터가 추가돼야 한다는 점이다.  
세번째 문제는 Connection 파라미터가 UserDao 인터페이스 메소드에 추가되면 UserDao는 더 이상 데이터 엑세스 기술에 독립적일 수가 없다는 점이다.  
JPA나 하이버네이트로 UserDao 구현 방식을 변경하려고 하면 Connection 대신 EntityManager나 Session 오브젝트를 UserDao 메소드가 전달받아야 한다.  
마지막으로 테스트 코드에도 영향을 끼친다. 지금까지 DB 커넥션은 전혀 신경 쓰지 않고 테스트에서 UserDao를 사용할 수 있었는데 이제는 테스트 코드에서 집적 Connection 오브젝트를  
일일이 만들어서 DAO 메소드를 호출하도록 모두 변경해야 한다.  

#### Connection 파라미터 제거
upgradeLevels() 메소드가 트랜잭션 경계설정을 해야 한다는 사실은 피할 수 없다. 따라서 그 안에서 Connection을 생성하고 트랜잭션 시작과 종료를 관리하게 한다.  
대신 여기서 생성된 Connection을 계속 메소드의 파라미터로 전달하다가 DAO를 호출할 때 사용하게 하는 건 피하고 싶다.  
이를 위해 스프링이 제안하는 방법은 독립적인 트랜잭션 동기화 방식이다.

트랜잭션 동기화란 UserService에서 트랜잭션을 시작하기 위해 만든 Connection 오브젝트를 특별한 장소에 보관해두고, 이후에 호출되는 DAO의 메소드에서는 저장된 Connection을  
가져다가 사용하게 하는 것이다.

(1) UserService는 Connection을 생성하고  
(2) 이를 트랜잭션 동기화 저장소에 저장해두고 Connection의 setAutoCommit(false)를 호출해 트랜잭션을 시작시킨 후에 DAO의 기능을 사용한다.  
(3) 첫 번째 update() 메소드가 호출되고, update() 메소드 내부에서 이용하는 JdbcTemplate 메소드에서는 가장 먼저  
(4) 트랜잭션 동기화 저장소에 현재 시작된 트랜잭션을 가진 Connection 오브젝트가 존재하는지 확인한다.  
(5) Connection을 이용해 PreparedStatement를 만들어 SQL을 실행한다. 트랜잭션 동기화 저장소에서 DB 커넥션을 가져왔을 때는 JdbcTemplate은  
Connection을 닫지 않은 채로 작업을 마친다. 이렇게 해서 트랜잭션 안에서 첫 번째 DB 작업을 마쳤다. 여전히 Connection은 열려 있고 트랜잭션은 진행 중인 채로  
트랜잭션 동기화 저장소에 저장되어 있다.  
(6) 두번째 update()가 호출되면 이때도 마찬가지로  
(7) 트랜잭션 동기화 저장소에서 Connection을 가져와  
(8) 사용한다.  
(9) 마지막 update()도  
(10) 같은 트랜잭션을 가져와  
(11) 사용한다.  
(12) 트랜잭션 내의 작업이 정상적으로 끝났으면 UserService는 이제 Connection의 commit()을 호출해서 트랜잭션을 완료시킨다.  
(13) 마지막으로 트랜잭션 저장소가 더 이상 Connection 오브젝트를 저장하지 않도록 이를 제거한다.  

트랜잭션 동기화 저장소는 작업 스레드마다 독립적으로 Connection 오브젝트를 저장하고 관리하기 때문에 다중 사용자를 처리하는 서버의 멀티스레드 환경에서도 충돌이 날 염려는 없다.  

#### 트랜잭션의 동기화 적용  
멀티스레드 환경에서도 안전한 트랜잭션 동기화 방법을 구현하는 일이 기술적으로 간단하지 않은데 스프링은 JdbcTemplate과 더불어 이런 트랜잭션 동기화 기능을 지원하는  
간단한 유틸리티 메소드를 제공하고 있다.  
UserService에서 DB 커넥션을 직접 다룰 때 DataSource가 필요하므로 DataSource 빈에 대한 DI 설정을 해둬야 한다.  

스프링이 제공하는 트랜잭션 동기화 관리 클래스는 TransactionSynchronizationManager이다. 이 클래스를 이용해 먼저 트랜잭션 동기화 작업을 초기화하도록 요청한다.  
그리고 DataSourceUtils에서 제공하는 getConnection() 메소드를 통해 DB 커넥션을 생성한다. DataSource에서 직접 Connection을 가져오지 않고, 스프링이 제공하는 유틸리티 메소드를  
쓰는 이유는 이 DataSourceUtils의 getConnection() 메소드는 Connection 오브젝트를 생성해줄 뿐만 아니라 트랜잭션 동기화에 사용하도록 저장소에 바인딩해주기 때문이다.  
트랜잭션 동기화가 되어 있는 채로 JdbcTemplate을 사용하면 JdbcTemplate의 작업에서 동기화시킨 DB 커넥션을 사용하게 된다.

#### JdbcTemplate과 트랜잭션 동기화
지금까지 JdbcTemplate은 update()나 query() 같은 JDBC 작업의 템플릿 메소드를 호출하면 직접 Connection을 생성하고 종료하는 일을 모두 담당한다고 했다.  
JdbcTemplate은 만약 미리 생성돼서 트랜잭션 동기화 저장소에 등록된 DB 커넥션이나 트랜잭션이 없는 경우에는 JdbcTemplate이 직접 DB 커넥션을 만들고 트랜잭션을 시작해서  
JDBC 작업을 진행한다. 반면에 트랜잭션 동기화를 시작해놓았다면 그때부터 실행되는 JdbcTemplate 메소드에서는 직접 DB 커넥션을 만드는 대신 트랜잭션 동기화 저장소에 들어 있는  
DB 커넥션을 가져와 사용한다. 따라서 트랜잭션 적용 여부에 맞춰 UserDao 코드를 수정할 필요가 없다. JDBC 코드의 try/catch/finally 작업 흐름 지원,  
SQLException의 예외 변환과 함께 JdbcTemplate이 제공해주는 세 가지 유용한 기능 중 하나다.  

#### 기술과 환경에 종속되는 트랜잭션 경계설정 코드
G사의 새로운 요구사항은 하나의 트랜젹션 안에서 여러 개의 DB에 데이터를 넣는 작업을 해야 할 필요가 발생했다.  
한 개 이상의 DB로의 작업을 하나의 트랜잭션으로 만드는 건 JDBC의 Connection을 이용한 트랜잭션 방식인 로컬 트랜잭션으로는 불가능하다.  
왜냐하면 로컬 트랜잭션은 하나의 DB connection에 종속되기 때문이다. 따라서 각 DB와 독립적으로 만들어지는 Connection을 통해서가 아니라, 별도의 트랜잭션 관리자를 통해  
트랜잭션을 관리하는 글로벌 트랜잭션 방식을 사용해야 한다. 글로벌 트랜잭션을 적용해야 트랜잭션 매니저를 통해 여러 개의 DB가 참여하는 작업을 하나의 트랜잭션으로 만들 수 있다.  

자바에서는 JTA를 이용해 트랜잭션 매니저를 활용하면 여러 개의 DB나 메시징 서버에 대한 작업을 하나의 트랜잭션으로 통합하는 분산 트랜잭션 또는 글로벌 트랜잭션이 가능해진다.  

문제는 JTA를 사용하여 글로벌 트랜잭션 관리하는 코드와 로컬 트랜잭션을 관리하는 코드가 다르기 때문에 코드의 변경이 필요하다. 그리고 Y사에서는 하이버네티어를 이용해 UserDao를 직접 구현했다고 알려왔다.  
하이버네이트는 Connection을 직접 사용하지 않고 Session이라는 것을 사용하고, 독자적인 트랜잭션 관리 API를 사용한다.

Connection을 이용한 트랜잭션 코드가 UserService에 등장하면서부터 UserService는 UserDaoJdbc에 간접적으로 의존하는 코드가 되어버렸다.  
다행히도 트랜잭션의 경계설정을 담당하는 코드는 일정한 패턴을 갖는 유사한 구조다. 사용 방법에 공통점이 있다면 추상화를 생각해볼 수 있다.  
이 공통점을 뽑아내 추상화한 것이 JDBC다. JDBC라는 추상화 기술이 있기 때문에 자바의 DB 프로그램 개발자는 DB의 종류에 상관없이 일관된 방법으로 데이터 엑세스 코드를 작성할 수 있었다.  

스프링이 제공하는 트랜잭션 경계설정을 위한 추상 인터페이스는 PlatformTransactionManager다. JDBC의 로컬 트랜잭션을 이용한다면 platformTransactionManager를 구현한  
DataSourceTransactionManager를 사용하면 된다. DefaultTransactionDefinition 오브젝트는 트랜잭션에 대한 속성을 담고 있다.  
이렇게 시작된 트랜잭션은 TransactionStatus 타입의 변수에 저장된다. 트랜잭션 작업을 모두 수행한 후에는 트랜잭션을 만들 때 돌려받은 TransactionStatus 오브젝트를  
파라미터로 해서 PlatformTransactionManager의 commit() 메소드를 호출하면 된다.  

JTA를 이용하는 글로벌 트랜잭션으로 변경하려면 PlatformTransactionManager의 구현체를 JTATransactionManager로 바꿔주면 된다.  
UserService 코드가 어떤 트랜잭션 매니저를 구현 클래스를 사용할지 알고 있는 것은 DI 원칙에 위배된다.  
UserService에 수정자를 통해 PlatformTransactionManager 인터페이스 타입의 인스턴스를 주입받도록 한다.


## 5.3 서비스 추상화와 단일 책임 원칙
이렇게 기술과 서비스에 대한 추상화 기법을 이용하면 특정 기술환경에 종속되지 않는 포터블한 코드를 만들 수 있다.  
UserService와 UserDao는 어플리케이션의 로직을 담고 있는 어플리케이션 캐층이다. UserDao와 UserService는 인터페이스와 DI를 통해 연결됨으로써 결합도가 낮아졌다.  
UserDao는 DB 연결을 생성하는 방법에 대해 독립적이다. DataSource 인터페이스와 DI를 통해 추상화된 방식으로 로우레벨의 DB 연결 기술을 사용하기 때문이다.  
마찬가지로 UserService와 트랜잭션 기술과도 스프링이 제공하는 PlatformTransactionManager 인터페이스를 통한 추상화 계층을 사이에 두고 사용하게 했기 때문에,  
구체적인 트랜잭션 기술에 독립적인 코드가 됐다.  
DI의 가치는 이렇게 관심, 책임, 성격이 다른 코드를 깔끔하게 분리하는 데 있다.  

#### 단일 책임 원칙
이런 적절한 분리가 가져오는 특징은 객체지향 설계의 원칙 중의 하나인 단일 책임 원칙으로 설명할 수 있다.  
단일 책임 원칙은 하나의 모듈은 한 가지 책임을 가져야 한다는 의미다. 하나의 모듈이 바뀌는 이유는 한 가지여야한다고 설명할 수도 있다.  

UserService는 어떻게 사용자 레벨을 관리할 것인가와 어떻게 트랜잭션을 관리할 것인가라는 두 가지 책임을 갖고 있었다.  
사용자의 레벨 업그레이드 정책과 같은 사용자 관리 로직이 바뀐다면 수정을 해야되고, 트랜잭션 기술이 변경되면 또 수정을 해야한다.  
변경의 이유가 2가지 이기 때문에 단일 책임 원칙을 지키지 못하는 것이다.  

#### 단일 책임 원칙의 장점
단일 책임 원칙을 잘 지키고 있다면 어떤 변경이 필요할 때 수정 대상이 명확해진다. 단일 책임 원칙을 위한 핵심적인 도구가 바로 스프링이 제공하는 DI다.  
스프링의 DI가 없었다면 인터페이스를 도입해서 나름 추상화를 했더라도 적지 않은 코드 사이의 결합이 남아 있게 된다.  
new DataSourceTransactionManager라는 구체적인 의존 클래스 정보가 드러나는 코드가 존재한다면 인터페이스로 추상화를 안 했을 때보다는 훨씬 적긴 하겠지만  
로우레벨 기술의 변화가 있을 때마다 비즈니스 로직을 담은 코드의 수정이 발생한다. 결국 DI를 통해 PlatformTransactionManager의 생성과 의존관계 설정을 스프링에 맡긴 덕에  
완벽하게 트랜잭션 기술에서 자유로운 UserService를 가질 수 있게 된 것이다.  

객체지향 설계와 프로그래밍의 원칙은 서로 긴밀하게 관련이 있다. 단일 책임 원칙을 잘 지키는 코드를 만들려면 인터페이스를 도입하고 이를 DI로 연결해야 하며,  
그 결과로 단일 책인 원칙 뿐 아니라 개방 폐쇄 원칙도 잘 지키고, 모듈 간에 결합도가 낮아서 서로의 변경이 영향을 주지 않고, 같은 이유로 변경이 단일 책임에 직중되는 응집도 높은 코드가 나온다.  
이런 과정에서 전략 패턴, 어댑터 패턴, 브리지 패턴, 미디에이터 패턴 등 많은 디자인 패턴이 자연스럽게 적용되기도 한다.  
객체 지향 설계 원칙을 잘 지켜서 만든 코드는 테스트하기도 편하다.





