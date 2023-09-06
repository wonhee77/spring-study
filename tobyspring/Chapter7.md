# 7장 스프링 핵심 기술의 응용
지금가지 스프링의 3대 기술인 IoC/DI, 서비스 추상화, AOP에 대해 간단히 살펴봤다. 스프링이 가장 가치를 두고 적극적으로 활용하려고 하는 것은 결국 자바 언어가  
기반을 두고 있는 객체지향 기술이다. 스프링의 모든 기술은 결국 객체지향적인 언어의 장점을 적극적으로 활용해서 코드를 작성하도록 도와주는 것이다.  

## 7.1 SQL과 DAO의 분리
UserDao에서 SQL을 DAO에서 분리하고 싶다. DB 테이블과 필드 정보를 고스란히 담고 있는 SQL이 여전히 남아있기 때문이다.  
개발 중에는 물론이고 운영 중인 시스템의 SQL도 변경이 된다. 그때마다 DAO코드를 수정하고 이를 다시 컴파일해서 적용하는 건 번거로울 뿐만 아니라 위험하기도 하다.  
SQL이 코드상에 존재하기 때문에 잘못 작성될 가능성이 크다. 그래서 7장에서는 SQL을 DAO에서 분리하는 작업에 도전해본다. 

#### XML 설정을 이용한 분리
스프링은 설정을 이용해 빈에 값을 주입해줄 수 있다. SQL은 문자열로 되어 있으니 설정파일에 프로퍼티 값으로 정의해서 DAO에 주입해줄 수 있다. 

UserDaoJdbc 클래스의 SQL 6개를 프로퍼티로 만들고 이를 XML에서 지정하도록 해보자. 먼저 add() 메소드에서 사용할 SQL을 프로퍼티로 정의한다.  
```java
public class UserDaoJdbc implements UserDao {
    private String sqlAdd;
    
    public void setSqlAdd(String sqlAdd) {
        this.sqlAdd = sqlAdd;
    }
}
```

다음은 XML 설정의 userDao 빈에 sqlAdd 프로퍼티를 추가하고 SQL을 넣어준다.  
```xml
<bean id="userDao" class="springbook.user.dao.UserDaoJdbc">
    <property name="dataSource" ref="dataSource" />
    <property name="sqlAdd" value="insert into users(id, name, password, email, level, login, recommend)
        values(?,?,?,?,?,?,?)" />
</bean>
```

SQL이 점점 많아지면 그때마다 DAO에 DI용 프로퍼티를 추가하기가 상당히 귀찮다. 그래서 이번에는 SQL을 하나의 컬렉션으로 담아두는 방법을 시도해보자. 맵을 이용하면  
키 값을 이용해 SQL 문장을 가져올 수 있다. Map 타입의 sqlMap 프로퍼티를 대신 추가한다.  

```java
public class UserDaoJdbc implements UserDao {
    private Map<String, String> sqlMap;
    public void setSqlMap(Map<String, String> sqlMap) {
        this.sqlMap = sqlMap;
    }
}
```

property 태그의 value 애트리뷰트로는 Map 타입을 정의할 수 없다. 그래서 스프링이 제공하는 <map> 태그를 사용해야 한다.

```java
    <property name="sqlMap">
        <map>
            <entry key="add" value=""/>
        </map>
```

#### SQL 제공 서비스
SQL과 DI 정보가 섞여 있으면 보기에도 지저분하고 관리하기에도 좋지 않다. SQL을 따로 분리해둬야 독립적으로 SQL 문의 리뷰나 튜닝 작업을 수행하기 편리한다.  
SQL 제공 기능을 본격적으로 분리해서 다양항 SQL 정보 소스를 사용할 수 있고, 운영 중에 동적으로 갱신도 가능한 유연하고 확장성이 뛰어난 SQL 서비스를 만들어 보자.  

가장 먼저 할 일은 SQL 서비스의 인터페이스를 설계하는 것이다.  
```java
public interface SqlService {
    String getSql(String key) throws SqlRetirievalFailureException;
}
```

예외의 원인을 구분해야 한다면 SqlRetrievalFailureException의 서브 클래스를 만들어 사용하면 된다.  
```java
public class SqlRetirevalFailureException extends RuntimeException {

    public SqlRetirevalFailureException(String message) {
        super(message);
    }
    
    public sqlRetrievalFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

이제 UserDaoJdbc는 SqlService 인터페이스를 통해 필요한 SQL을 가져와 사용할 수 있게 SqlService 프로퍼티를 추가하고 수정자로 DI 받을 수 있게 한다.  

SqlService를 구현해보자. 앞에서 키와 SQL을 엔트리로 갖는 맵을 빈 설정에 넣었던 방법을 sqlService에도 그대로 적용할 수 있다. Map 타입의 프로퍼티를 추가하고,  
맵에서 SQL을 읽어서 돌려주도록 SqlService의 getSql() 메소드를 구현한다. 이제 DAO의 수정없이도 편리하고 자유롭게 SQL 서비스 구현을 발전시켜나갈 수 있다.  
어야 한다.