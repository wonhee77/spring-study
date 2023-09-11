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

## 7.2 인터페이스의 분리와 자기참조 빈

### 요약
이제 SqlService 인터페이스의 구현 방법을 고민해보자. 

#### XML 파일 매핑
검색용 키와 SQL 문장 두 가지를 담을 수 있는 간단한 XML 문서를 설계해보고, 이 XML 파일에서 SQL을 읽어뒀다가 DAO에게 제공해주는 SQL 서비스 구현 클래스를  
만들어 보자. 

JAXB  
XML에 담긴 정보를 파일에서 읽어오는 다양한 방법 중에 JAXB(JAVA Architecture for XML Binding)을 이용해 보자. JDK6 이라면 java.xml.bind 패키지  
안에서 JXAB 구현 클래스를 찾을 수 있다.  
XML을 작성한 다음 컴파일을 하면 지정한 경로에 SqlType.java, SqlMap.java 2개의 클래스 파일이 생성된다.  
XML 문서를 읽어서 자바의 오브젝트로 변환하는 것을 JAXB에서는 언마샬링이라고 부른다. 반대로 바딩일 오브젝트를 XML 문서로 변환하는 것은 마샬링이라고 한다.  

언제 JAXB를 사용해 XML 문서를 가져올지 생각해봐야 한다. DAO가 SQL을 요청할 때마다 매번 XML 파일을 다시 읽어서 SQL을 찾는 건 너무 비효율적인 방법이다.  
생성자 초기화 방법을 사용해 언마셜링을 하고, 데이터들을 쉽게 찾을 수 있도록 Map 자료구조에 저장을 하자.

생성자에서 예외가 발생할 수도 있는 복잡한 초기화 작업을 다루는 것은 좋지 않다. 오브젝트를 생성하는 중에 생성자에서 발생하는 예외를 다루기 힘들고, 상속하기 불편하며,  
보안에도 문제가 생길 수 있다. 일단 초기 상태를 가진 오브젝트를 만들어 놓고 별도의 초기화 메소드를 사용하는 방법이 바람직하다.  
또 다른 문제점은 읽어들일 파일의 위치와 이름이 코드에 고정되어 있다는 점을 들 수 있다. 코드의 로직과 여타 이유로 바뀔 가능성이 있는 내용은 외부에서 DI로   
설정해줄 수 있게 만들어야 한다.  

파일 이름을 외부에서 주입받을 수 있도록 수정자를 추가하고, 생성자에서 하던 일을 loadSql()이라는 별도의 메소드로 분리하자.  
하지만 loadSql()이라는 초기화 메소드는 언제 실행돼야 할까? 또, 어떻게 실행시킬 수 있을까?  
이 XmlSqlService 오브젝트에 대한 제어권이 우리가 만드는 코드에 있다면, 오브젝트를 만드는 시점에서 초기화 메소드를 한 번 호출해주면 된다.  
스프링은 빈 오브젝트를 생성하고 DI 작업을 수행해서 프로퍼티를 모두 주입해준 뒤에 미리 지정한 초기화 메소드를 호출해주는 기능을 갖고 있다.  
AOP를 살펴볼 때 스프링의 빈 후처리기에 대해 설명했다. 빈 후처리기는 스프링 컨테이너가 빈을 생성한 뒤에 부가적인 작업을 수행할 수 있게 해주는 특별한 기능이다.  
AOP를 위한 프록시 자동생성기가 대표적인 빈 후처리기다. 프록시 자동생성기 외에도 스프링이 제공하는 여러 가지 빈 후처리기가 존재한다. 그중에서 애노테이션을 이용한  
빈 설정을 지원해주는 몇 가지 빈 후처리기가 있다. 이 빈 후처리기는 <bean> 태그를 이용해 하나씩 등록할 수도 있지만, 그보다는 context 스키마의  
annotation-config 태그를 사용하면 더 편리하다. context 네임스페이스를 사용해서 `<context:annotation-config/>` 태그를 만들어 설정파일에 넣어주면  
빈 설정 기능에 사용할 수 있는 특별한 애노테이션 기능을 부여해주는 빈 후처리기들이 등록된다.  

여기서 사용할 애노테이션을 @PostConstruct다. 스프링은 @PostConstruct 애노테이션을 빈 오브젝트의 초기화 메소드를 지정하는 데 사용한다.  
생성자와는 달리 프로퍼티까지 모두 준비된 후에 실행된다는 명에서 @PostConstruct 초기화 메소드는 매우 유용하다.

XML 대신 다른 포맷의 파일에서 SQL을 읽어오게 하거나 Map타입이 아닌 다른 방식으로 저장해두고 이를 검색해서 가져오려면 지금까지 만든 코드를 직접 고치거나 새로  
만들어야 한다. 

#### 책임에 따른 인터페이스 정의
가장 먼저 할 일은 분리 가능한 관심사를 구분해보는 것이다. XmlSqlService 구현을 참고해서 독립적으로 변경 가능한 책임을 뽑아보자.  
첫째는 SQL 정보를 외부의 리소스로부터 읽어오는 것이다. 리소스는 단순한 텍스트 파일일 수도 있고, 미리 정의된 스키마를 가진 XML 일수도 있고, 엑셀파일일 수도 있고,  
DB일 수도 있다.  
두번째 책임은 읽어온 SQL을 보관해두고 있다가 필요할 때 제공해주는 것이다.  
여기에 좀 더 부가적인 책임을 생각해볼 수 있다. 일단 서비스를 위해서 한 번 가져온 SQL을 필요에 따라 수정할 수 있게 하는 것이다.  

DAO 관점에서는 SqlService라는 인터페이스를 구현한 오브젝트만 의존하고 있으므로 달라질 것은 없다. 대신 SqlService의 구현 클래스가 변경 가능한 책임을 가진  
SqlReader와 SqlRegistry 두 가지 타입의 오브젝트를 사용하도록 만든다. 당연히 인터페이스를 이용하게 하고, DI를 통해 의존 오브젝트를 제공받게 해야한다.  
SqlReader가 읽어오는 SQL 정보는 다시 SqlRegistry에 전달해서 등록하게 해야 한다.  
SqlReader가 제공하는 메소드의 리턴 타입은 무엇으로 해야 할까? SqlReader가 가져올 내용은 간단하니 간단히 SQL과 키를 쌍으로 하는 배열을 만들고, 이를 다시  
리스트에 담아서 가져오거나 지금가지 많이 써왔던 방식대로 맵을 이용할 수 있다.  

```java
Map<String, String> sqls = sqlReader.readSql();
sqlRegistry.addSql(sqls);
```

둘 사이에서 정보를 전달하기 위해 일시적으로 Map 타입의 형식을 갖도록 만들어야 하는 건 불편하다. 구현 방식이 다양한 2개의 오브젝트 사이에 복잡한 정보를 전달하기  
위해서 하나의 타입을 사용하는 것은 번거롭다.  
발상을 조금 바꿔보면 이러한 번거로움을 제거할 방법을 찾을 수 있다. SqlService가 일단 SqlReader에게서 정보를 전달 받은 뒤, SqlRegistry에 다시 전달해줘야  
할 필요는 없다. SqlService가 SqlReader에게 데이터를 달라고 요청하고, 다시 SqlRegistry에게 이 데이터를 사용하라고 하는 것보다는 SqlReader에게  
SqlRegistry 전략을 제공해주면서 이를 이용해 SQL 정보를 SqlRegistry에 저장하라고 요청하는 편이 낫다.

```java
sqlReader.readSql(sqlRgistry);

interface SqlRegistry {
    void registerSql(String key, String sql); // SqlReader는 읽어들인 SQL을 이 메소드를 이용해 레지스트리에 저장한다. 
}
```

이제 SqlReader와 SqlRegistry 인터페이스를 정의해보자.

```java
public interface SqlRegistry {
    void registerSql(String key, String sql);
    
    String findSql(String key) throws SqlNotFoundException;
}

public interface SqlReader {
    void read(SqlRegistry sqlRegistry);
}
```

#### 다중 인터페이스 구현과 간접 참조
SqlService의 구현 클래스는 이제 SqlReader와 SqlRegistry 두 개의 프로퍼티를 DI 받을 수 있는 구조로 만들어야 한다.  
인터페이스는 한 클래스에서 하나 이상을 구현할 수 있다. 하나의 클래스가 여러 개의 인터페이스를 상속해서 여러 종류의 타입으로서 존재할 수 있는 것이다.

#### 인터페이스를 이용한 분리
일단 XmlSqlService는 SqlService만을 구현한 독립적인 클래스라고 생각하자. DI를 통해 이 두 개의 인터페이스를 구현한 오브젝트를 주입받을 수 있도록 프로퍼티를  
정의한다. 다음은 XmlSqlService 클래스가 SqlRegistry와 SqlReader를 구현하도록 만들자. SqlRegistry와 SqlReader를 구현한 코드에서 서로의 내부  
정보에 접근하거나 하면 안된다. SqlReader에서 파라미터로 전달받은 SqlRegistry 타입 오브젝트가 사실 자기 자신이긴 하겠지만, 그래도 다른 오브젝트라고 생각하고  
인터페이스에 정의된 메소드를 통해서만 사용해야 한다. 

#### 자기참조 빈 설정
이제 빈 설정을 통해 실제 DI가 일어나도록 해야 한다. 클래스는 하나뿐이고 빈도 하나만 등록할 것이지만, 마치 세 개의 빈이 등록된 것처럼 SqlService빈이  
SqlRegistry와 SqlReader를 주입받도록 만들어야 한다.

```xml
<bean id="sqlService" class="springbook.user.sqlservice.XmlSqlService">
    <property name="sqlReader" ref="sqlService" />
    <property name="sqlRegistry" ref="sqlService" />
    <property name="sqlmapFile" value="sqlmap.xml" />
</bean>
```

스프링은 프로퍼티의 ref 항목에 자기 자신을 넣는 것을 허용한다. 이를 통해, sqlService를 구현한 메소드와 초기화 메소드는 외부에서 DI 된 오브젝트라고 생각하고  
결국 자신의 메소드에 접근한다. 

이제 확장 가능한 인터페이스를 정의하고 인터페이스에 따라 메소드를 구분해서 DI가 가능하도록 코드를 재구성하는 데 성공했다. 다음은 이를 완전히 분리해두고 DI로  
조합해서 사용하게 만드는 단계다. 

#### 디폴트 의존관계를 갖는 빈 만들기
특정 오브젝트가 대부분의 환경에서 거의 디폴트라고 해도 좋을 만큼 기본적으로 사용될 가능성이 있다면, 디폴트 의존관계를 갖는 빈을 만드는 것을 고려해볼 필요가 있다.  
디폴트 의존관계란 외부에서 DI 받지 ㅇ낳는 경우 기본적으로 자동 적용되는 의존관계를 말한다. DI 설정이 없는 경우 디폴트로 적용하고 싶은 의존 오브젝트를 생성자에서  
넣어준다.

JaxbXmlSqlReader의 sqlmapFile 프로퍼티가 비어있기 때문에 테스트를 실행시 에러가 발생한다. sqlmapFile의 경우도 JaxbXmlSqlReader에 의해 기본적으로  
사용될 만한 디폴트 값을 가질 수 있다. SQL 파일 이름을 매번 바꿔야 할 필요도 없고, 관례적으로 사용할 만한 이름을 정해서 디폴트로 넣어주면 된다.  

DI를 사용한다고 해서 항상 모든 프로퍼티 값을 설정에 넣고, 모든 의존 오브젝트를 빈으로 일일이 지정할 필요는 없다. DefaultSqlService처럼 자주 사용되는  
의존 오브젝트는 미리 지정한 디폴트 의존 오브젝트를 설정 없이도 사용할 수 있게 만드는 것도 좋은 방법이다.  

디폴트 의존 오브젝트를 사용하는 방법에는 단점이 한 가지 있다. 설정을 통해 다른 구현 오브젝트를 사용하게 해도 DefaultSqlService는 생성자에서 일단 디폴트 의존  
오브젝트를 다 만들어버린다는 점이다. 프로퍼티로 설정한 빈 오브젝트로 바로 대체되긴 하겠지만 사용되지 않는 오브젝트가 만들어진다. @PostConstruct 초기화 메소드를  
이용해 프로퍼티가 설정됐는지 확인하고 없는 경우에만 디폴트 오브젝트를 만드는 방법을 사용하면 해결할 수 있다.  


## 7.3 서비스 추상화 적용
JaxbXmlSqlReader는 크게 두 가지 과제로 더 발전시킬 수 있다.  
- JAXB 외에도 다양한 XML과 자바오브젝트를 매핑하는 기술이 있다. 필요에 따라 다른 기술로 손쉽게 교체할 수 있어야 한다.  
- XML 파일을 좀 더 다양한 소스에서 가져올 수 있게 만든다. 현재는 UserDao 클래스와 같은 클래스패스 안에서만 XML을 읽어올 수 있다. 이것을 임의의 클래스패스나  
파일 시스템 상의 절대위치 또는 HTTP 프로토콜을 통해 원격에서 가져오도록 확장할 수 없는가 하는 점이다.  
  
XML과 자바오브젝트를 매핑해서 상호 변환해주는 기술을 간단히 OXM(Object-XML Mapping) 이라고도 한다. OXM 프레임워크와 기술들은 기능 면에서 상위 호환성이  
있다. JAXB를 포함해서 다섯 가지 기술 모두 사용 목적이 동일하기 때문에 유사한 기능과 API를 제공한다. 기능이 같은 여러 가지 기술이 존재한다는 이야기가 나오면  
떠오르는게 있다. 바로 서비스 추상화다. 스프링은 OXM에서도 서비스 추상화를 제공한다. 

#### OXM 서비스 인터페이스
스프링이 제공하는 OXM 추상화 서비스 인터페이스에는 자바 오브젝트를 XML로 변환하는 Marshaller와 반대인 Unmarshaller가 있다. SqlReader는 이 중에서  
Unmarshaller를 이용하면 된다.

```java
public interface Unmarshaller {
    boolean support(Class<?> clazz);
    Object unmarshal(Source source) throws IOException, XmlMappingException
        // source를 통해 제공받은 XML을 자바오브젝트 트리로 변환해서 그 루트 오브젝트를 돌려준다.
}
```

JAXB를 이용하도록 만들어진 Unmarshaller 구현 클래스는 Jaxb2Marshaller이다. 이 클래스는 Marshaller와 Unmarshaller 인터페이스를 둘 다 구현한다.  

이제 스프링의 OXM 추상화 기능을 이용하는 SqlService를 만들어보자. 이름은 OxmSqlService라고 하고 SqlRegistry는 DI 받을 수 있게 만들지만 SqlReader는  
스프링의 OXM 언마샬러를 이용하도록 OxmSqlService 내에 고정시켜야 한다. SQL을 읽는 방법을 OXM으로 제한해서 사용성을 극대화하는게 목적이다.  

OxmSqlService는 BaseSqlService와 유사하게 SqlReader 타입의 의존 오브젝트를 사용하되 이를 스태틱 멤버 클래스로 내장하고 자신만이 사용할 수 있도록  
만들어보자. 의존 오브젝트를 자신만이 사용하도록 독점하는 구조로 만드는 방법이다. 밖에서 볼 때는 하나의 오브젝트로 보이지만 내부에서는 의존관계를 가진 두 개의  
오브젝트가 깔끔하게 결합돼서 사용된다. 유연성은 조금 손해를 보더라도 내부적으로 낮은 결합도를 유지한 채로 응집도가 높은 구현을 만들 때 유용하게 쓸 수 있는 방법이다.  

```java
public class OxmSqlService implements SqlService {
    private final OxmSqlReader oxmSqlReader = new OxmSqlReader();
    
    private class OxmSqlReader implements SqlReader {
        
    }
}
```

OxmlSqlReader는 외부에 노출되지 않기 때문에 OxmSqlService에 의해서만 만들어지고, 스스로 빈으로 등록될 수 없다. 따라서 자신이 DI를 통해 제공받아야 하는  
프로퍼티가 있다면 이를 OxmSqlService의 공개된 프로퍼티를 통해 간접적으로 DI 받아야 한다. 

```java
public class OxmlSqlService implements SqlService {
    private final OxmSqlReader oxmSqlReader = new OxmlSqlService();
    
    public void setUnmarshaller(Unmarshaller unmarshaller) {
        this.oxmSqlReader.setUnmarshallser(unmarshaller);
    }

    public void setSqlmapFile(String sqlmapFile) {
        this.oxmSqlReader.setSqlmapFile(sqlmapFile);
    }
    
    private class OxmSqlReader implements SqlReader {
        private Unmarshaller unmarshaller;
        private String sqlmapFile;
        // setter
    }
}
```

#### 위임을 이용한 BaseSqlService의 재사용
loadSql()과 getSql()이라는 SqlService의 핵심 메소드 구현 코드가 BaseSqlService와 동일하다. 프로퍼티 설정을 통한 초기화 작업을 제외하면 두 가지  
작업의 코드는 BaseSqlService와 OxmSqlService 양쪽에 중복된다. 지금은 로직이 간단하지만 작업이 꽤나 복잡해지면 코드의 중복은 심각한 문제가 될 수도 있다.  
loadSql()과 getSql()의 구현 로직은 BaseSqlService에만 두고, OxmSqlService는 일종의 설정과 기본 구성을 변경해주기 위한 어댑터 같은 개념으로  
BaseSqlService 앞에 두는 설계가 가능하다. OxmSqlService의 외형적인 틀은 유지한 채로 SqlService의 기능 구현은 BaseSqlService로 위임하는 것이다.  

부가기능 프록시처럼 많은 타깃에 적용할 것도 아니고, 특화된 서비스를 위해 한 번만 사용할 것이므로 유연한 DI 방식은 포기하고 OxmSqlService와 BaseSqlService를  
한 클래스로 묶는 방법을 생각해보자. 마치 OxmSqlReader를 OxmSqlService에 내장하고 있는 것과 마찬가지 방법을 사용하면 된다. 

```java
public class OxmSqlService implements SqlService {
    private final BaseSqlService baseSqlService = new BaseSqlService();
    
    @PostConstruct
    public void loadSql(){
        this.baseSqlService.setSqlReader(this.oxmSqlReader);
        this.baseSqlService.setSqlRegistry(this.sqlRegistry);
        this.baseSqlService.loadSql();
    }
    
    public String getSql(String key) throws SqlRetrievalFailureException {
        return this.baseSqlService.getSql(key)
    }
}
```

#### 리소스 추상화
기존 OxmSqlReader는 클래스패스로부터 리소스를 가져오기 위해 ClassLoader 클래스의 getResourceAsStream() 메소드를 사용했다. 그래서 리소스가  
클래스패스에 존재하는 파일로 제한된다. 리소스의 위치와 종류에 따라서 다른 클래스와 메소드를 사용해야 한다는 점이 불편하다. 그렇다면 OXM과 마찬가지로 서비스 추상화를  
적용할 수 있지 않을까? 

스프링은 자바에 존재하는 일관성 없는 리소스 접근 API를 추상화해서 Resource라는 추상화 인터페이스를 정의했다. 어플리케이션 컨텍스트가 사용할 설정정보 파일을  
지정하는 것부터 시작해서 스프링의 거의 모든 API는 외부의 리소스 정보가 필요할 때는 항상 이 Resource 추상화를 이용한다.

스프링에는 URL 클래스와 유사하게 접두어를 이용해 Resource 오브젝트를 선언하는 방법이 있다. 문자열 안에 리소스의 종류와 리소스의 위치를 함께 표현하게 해주는  
것이다. 그리고 이렇게 문자열로 정의된 리소스를 실제 Resource 타입 오브젝트로 변환해주는 ResourceLoader를 제공한다.

```java
public interface ResourceLoader {
    Resource getResource(String location);
}
```

ResourceLoader의 대표적인 예는 바로 스프링의 애플리케이션 컨텍스트이다. 애플리케이션 컨텍스트가 구현해야 하는 인터페이스인 ApplicationContext는  
ResourceLoader 인터페이스를 상속하고 있다. 따라서 모든 애플리케이션 컨텍스트는 리소스 로더이기도 하다.  
스프링 컨테이너는 리소스 로더를 다양한 목적으로 사용하고 있다. 어플리케이션 컨텍스트가 사용하할 스프링 설정정보가 담긴 XML 파일도 리소스 로더를 이용해 Resource  
형태로 읽어온다. 


#### 7.4 인터페이스 상속을 통한 안전한 기능확장

### 요약
애플리케이션을 새로 시작하지 않고 특정 SQL의 내용만을 변경하고 싶다면 어떻게 해야할지 생각해보자.

#### DI를 의식하는 설계
DI를 적용하러면 커다란 오브젝트 하나만 존재해서는 안된다. 최소한 두 개 이상의 의존관계를 가지고 서로 협력해서 일하는 오브젝트가 필요하다. 그래서 적절한 책임에 따라  
오브젝트를 분리해줘야 한다. 그리고 항상 의존 오브젝트는 자유롭게 확장될 수 있다는 점을 염두에 둬야 한다. DI는 확장을 위해 필요한 것이므로 항상 미래에 일어날  
변화를 예상하고 고민해야 적합한 설계가 가능해진다. DI란 결국 미래를 프로그래밍하는 것이다.

#### DI와 인터페이스 프로그래밍
DI를 위해 인터페이스를 사용하는 첫 번째 이유는 다형성을 얻기 위해서이다. 하나의 인터페이스를 통해 여러 개의 구현을 바꿔가며 사용할 수 있게 하는 것이 DI가 추구하는  
첫 번째 목적이다. 이 외에도 프록시, 데코레이터, 어댑터, 테스트 대역 등의 다양한 목적을 위해 인터페이스가 사용된다.  
DI의 목적이 단지 다형성을 편리하게 적용하는 것 때문만이라면 상속을 사용해도 된다. 그럼에도 인터페이스를 사용해야 하는 이유가 또 있다면 그것은 인터페이스 분리 원칙을  
통해 클라이언트와 의존 오브젝트 사이의 관계를 명확하게 해줄 수 있기 때문이다.  

자바의 클래스는 하나 이상의 인터페이스를 구현할 수 있다. 인터페이스는 하나의 오브젝트가 여러 개를 구현할 수 있으므로, 하나의 오브젝트를 바라보는 창이 여러가지일 수  
있다는 의미다. 각기 다른 관심과 목적을 가지고 어떤 오브젝트에 의존하고 있을 수 있다. 

오브젝트가 그 자체로 충분히 응집도가 높은 작은 단위로 설계됐더라도, 목적과 관심이 각기 다른 클라이언트가 있다면 인터페이스를 통해 이를 적절하게 분리해줄 필요가 있고,  
이를 객체지향 설계 원칙에서는 인터페이스 분리 원칙이라고 부른다. 인터페이스를 사용하지 않고 클래스를 직접 참조하는 방식으로 DI를 했다면, 인터페이스 분리 원칙과 같은  
클라이언트에 특화된 의존관게를 만들어낼 방법 자체가 없는 것이다. DI는 특별한 이유가 없는 한 항상 인터페이스를 사용한다고 기억해두자. 

#### 인터페이스 상속
하나의 오브젝트가 구현하는 인터페이스를 여러 개 만들어서 구분하는 이유 중의 하나는 오브젝트의 기능이 발전하는 과정에서 다른 종류의 클라이언트가 등장하기 때문이다.  
때로는 인터페이스를 여러 개 만드는 대신 기존 인터페이스를 상속을 통해 확장하는 방법도 사용된다.  

SqlService의 기본 구현체인 BaseSqlService는 SqlReader, SqlRegistry라는 두 개의 인터페이스를 통해 의존 오브젝트들을 DI 하도록 되어 있다.  
SqlRegistry의 구현체인 MySqlRegistry에 이미 등록된 SQL을 변경할 수 있는 기능을 넣어서 확장하고 싶다. 이때 이미 SqlRegistry 인터페이스를 이용해  
접근하는 클라이언트인 BaseSqlService 클래스와 그 서브클래스가 존재하기 때문에 SqlRegistry 인터페이스 자체를 수정하는 건 바람직한 방법이 아니다.  
BaseSqlService는 SqlRegistry 인터페이스가 제공하는 기능이면 충분하기 때문이다. DAO를 위한 SQL 조회 서비스인 BaseSqlService 입장에서 SQL을  
업데이트하는 기능을 이용하는 클라이언트가 될 이유가 없다.  
SQL 저장소에 담긴 SQL 정보를 변경하는 기능을 추가하기로 했다는건 그런 기능을 사용할 클라이언트가 존재해야 한다는 의미이다. 그러기 위해서는 기존의 SqlRegistry  
인터페이스를 상속하고 메소드를 추가해서 새로운 인터페이스로 정의돼야 한다.

```java
public interface UpdatableSqlRegistry extends SqlRegistry {
    public void updateSql(String key, String sql) throws SqlUpdateFailureException;
    
    public void updateSql(Map<String, String> sqlMap) throws SqlUpdateFailureException;
}
```

이렇게 인터페이스를 추가하거나 상속을 통해 확장하는 방식을 잘 활용하면 이미 기존의 인터페이스를 사용하는 클라이언트가 있는 경우에도 유연한 확장이 가능해진다.  
만약 SQL 수정 기능만을 처리하는 클라이언트가 필요했다면 기존 SqlRegistry 인터페이스를 상속하지 않고 새로운 인터페이스를 추가했을 수도 있다. 중요한 것은  
클라이언트가 정말 필요한 기능을 가진 인터페이스를 통해 오브젝트에 접근하도록 만들었는가이다.  
잘 적용된 DI는 결국 잘 설계된 오브젝트 의존관계에 달려 있다. 인터페이스를 적절하게 분리하고 확장하는 방법을 통해 오브젝트 사이의 의존관계를 명확하게 해주고,  
기존 의존관계에 영향을 주지 않으면서 유연한 확장성을 얻는 방법이 무엇인지 항상 고민해야 한다.


## 7.5 DI를 이용해 다양한 구현 방법 적용하기

### 요약
인터페이스 상속을 통한 확장과 클라이언트에 따른 인터페이스 분리 원칙을 잘 지키는 구조를 만들었으니 이제는 실제 구현을 해볼 차례다. 운영 중인 시스템에서 사용하는  
정보를 실시간으로 변경하는 작업을 만들 때 가장 먼저 고려해야 할 사항은 동시성 문제다. UpdatableSqlRegistry라는 인터페이스를 정의해 수정 가능한 SQL  
레지스트리 구현을 다양하게 DI로 적용할 수 있게 만들어뒀으니, 이를 활용해 여러 가지 구현을 만들어 적용해보자.

멀티스레드 환경에서 안전하게 HashMap을 조작하려면 Collections.synchronizedMap() 등을 이용해 외부에서 동기화해줘야 한다. 하지만 외부에서 동기화 해준다면  
요청이 많은 고성능 서비스에서는 성능에 문제가 생긴다. 그래서 동기화된 해시 데이터 조작에 최적화되도록 만들어진 ConcurrentHashMap을 사용하는 방법이 권장된다.  
ConcurrentHashMap은 데이터 조작 시 전체 데이터에 대한 락을 걸지 않고 조회는 락을 아예 사용하지 않는다.  

Jdbc 스키마 전용 태그를 사용하여 내장 database를 설정한다. 그리고 EmbeddedDbSqlRegistry를 만든다.

#### 트랜잭션 적용
SQL문 여러개를 수정하다가 중간에 에러가 발생하면 몇가지만 반영되고, 그 이후부터는 반영이 되지 않기 때문에 트랜잭션이 필요하다. 스프링에서 트랜잭션을 적용할 때  
트랜잭션 경계가 DAO 밖에 있고 범위가 넣ㅂ은 경우라면 AOP를 이용하는 것이 편리하다. 하지만 SQL 레지스트리라는 제한된 오브젝트내에서 서비스에 특화된 간단한  
트랜잭션이 필요한 경우라면 AOP와 같이 거창한 방법 보다는 간단히 트랜잭션 추상화 API를 직접 사용하는 게 편리할 것이다. 

테스트에서 EmbeddedDbSqlRegistry의 updateSql(Mapsqlmap) 메소드에 트랜잭션 기능을 추가해보자. PlatformTransactionManager를 직접 사용해서  
트랜잭션 처리 코드를 만들어도 되지만 그보다는 간결하게 트랜잭션 적용 코드에 템플릿/콜백 패턴을 적용한 TransactionTemplate을 쓰는 편이 낫다.

EmbeddedDbSqlRegistry가 DataSource를 DI 받아서 트랜잭션 매니저와 템플릿을 만들게 한다. 


## 7.6 스프링 3.1의 DI

### 요약

#### 자바 언어의 변화와 스프링
스프링이 제공하는 모든 기술의 기초가 되는 DI의 원리는 변하지 않았지만 DI가 적용된 코드를 작성할 때 사용하는 핵심 도구인 자바 언어에는 그간 적지 않은 변화가 있었다.  
이런 변화들이 DI 프레임워크로서 스프링의 사용 방식에도 여러 가지 영향을 줬다.

애노티에션 메타정보 활용
첫번째는 자바 코드의 메타정보를 이용한 프로그래밍 방식이다. 자바는 소스코드가 컴파일된 후 클래스 파일에 저장됐다가, JVM에 의해 메모리로 로딩되어 실행된다. 그런데  
때로는 자바 코드가 실행되는 것이 목적이 아니라 다른 자바 코드에 의해 데이터처럼 취급되기도 한다. 이런 스타일의 프로그래밍 방식은 자바 5와 JavaEE5가 등장한  
시점부터 급격하게 확산되기 시작했다. 자바 클래스나, 인터페이스, 필드, 메소드 등의 메타정보를 살펴보거나 조작하기 위해 java.lang 패키지 아래에 있는 리플렉션  
API를 사용하는 방식은 자바 초기 버전부터 존재해왔다. 원래 리플렉션 API는 자바 코드나 컴포넌트를 작성하는 데 사용되는 툴을 개발할 때 이용하도록 만들어졌는데,  
언제부턴가 본래 목적보다는 자바 코드의 메타정보를 데이터로 활용하는 스타일의 프로그래밍 방식에 더 많이 활용되고 있다. 이런 프로그래밍 방식의 벌정은 자바 5에서  
등장한 애노테이션일 것이다.  
애노테이션은 옵션에 따라 컴파일된 클래스에 존재하거나 애플리케이션이 동작할 때 메모리에 로딩되기도 하지만 자바 코드가 실행되는데 집적 참여하지 못한다. 복잡한  
리플렉션 API를 이용해 애노테이션의 메타정보를 조회하고, 애노테이션 내에 설정된 값을 가져와 참고하는 방법이 전부다.  

이렇게 애노테이션의 활용이 늘어난 이유는 애노테이션이 애플리케이션을 핵심 로직을 담은 자바 코드와 이를 지원하는 IoC 방식의 프레임워크, 그리고 프레임워크가 참조하는  
메타정보라는 세 가지로 구성하는 방식에 잘 어울리기 때문일 것이다. 애노테이션은 프레임워크가 참조하는 메타정보로 사용되기에 여러 가지 유리한 점이 많다.

1장에서 살펴본 DaoFactory는 처음엔 평범한 자바 코드로 작성됐다. 그런데 UserDao 한 가지가 아니라 애플리케이션을 구성하는 많은 오브젝트의 관계를 IoC/DI를  
이용해서 프레임워크와 메타정보를 활용하는 방식으로 작성하도록 발전시키려면 DaoFactory와 같은 단순한 자바 코드로 만들어진 관계 설정 책임을 담은 코드는 불편하다.  
그래서 XML로 전환해서 좀 더 간결한 형태로 전환했다. 

그런데 애노테이션이 등장하면서 상황이 달라졌다. 애노테이션은 XML이나 여타 외부 파일과 달리 자바 코드의 일부로 사용된다. 코드의 동작에 직접 영향을 주지 못하지만  
메타정보로서 활용되는 데는 XML에 비해 유리한 점이 많다.

애노테이션은 부여된 클래스의 패키지, 클래스 이름, 접근 제한자, 상속한 클래스나 구현 인터페이스가 무엇인지 알 수 있다. 단순한 애노테이션 하나를 자바 코드에 넣는  
것만으로도, 애노테이션을 참고하는 코드에서는 이렇게 다양한 부가 정보를 얻어낼 수 있다. 반면에 동일한 정보를 XML로 표현하려면 모든 내용을 명시적으로 나타내야 한다.  
코드를 작성할 양도 많고 텍스트 정보이기 때문에 오타가 발생하기 쉽다. 리팩토링 시에도 XML 의 패키지 이름을 다 고쳐야 한다.  

애노테이션의 단점도 있다. 자바 코드에 존재하기 때문에 변경할 때마다 매번 클래스를 컴파일 해줘야 한다.

정책과 관례를 이용한 프로그래밍
애노테이션 같은 메타정보를 활용하는 프로그래밍 방식은 코드를 이용해 명시적으로 동작 내용을 기술하는 대신 코드 없이도 미리 약속한 규칙 또는 관례를 따라서 프로그램이  
동작하도록 만드는 프로그래밍 스타일을 적극적으로 포용하게 만들어왔다.

이 절에서는 1장부터 지금까지 발전시켜왔던 사용자 DAO와 서비스 기능의 예제 코드를 스프링의 3.1의 최신 DI 스타일로 바꾸는 과정을 보여주고 설명한다.  

#### 자바 코드를 이용한 빈 설정
첫번째 작업은 XML을 없애는 것이다. 먼저 DI 정보로 사용될 자바 클래스를 만들어야 한다. DI 설정정보를 담은 클래스는 평범한 자바 클래스에 @Configuration  
애노테이션을 달아주면 만들 수 있다. TestApplicationContext 클래스를 만들어 @Configuration을 붙여주고 테스트 클래스에   
@ContextConfiguration(classes=TestApplicationContext.class)를 추가한다.

테스트를 성공시키려면 XML에 있던 빈 설정정보를 모두 TestApplicationContext로 옮겨야 한다. @ImportResource 애토네이션을 사용하면 자바 클래스에서  
XML의 설정정보를 가져오게 만들 수 있다. 

```java
@Configuration
@ImportResource("/test-applicationContext.xml")
public class TestApplicationContext {
    
}
```

#### bean의 전환
<bean>으로 정의된 DI 정보는 자바 코드, 특별히 @Bean이 붙은 메소드와 거의 1:1로 매핑된다. 
필드 이름과 일치하는 빈 아이디를 가진 빈을 주입받을 때는 @Resource를 사용하면 된다.  

<tx:annotation-driven /> 태그를 대채하려면 4개의 클래스를 빈으로 등록해야한다. 이 클래스들은 기억하기도 어렵고 어떻게 설정해야할지 파악하기도 힘들다.  
XML에서는 용도가 잘 설명되는 전용 태그로 정의돼서 손쉽게 사용했는데 자바 코드에서는 복잡한 로우 레벨의 클래스를 여러 개나 사용해서 빈을 정의해야 한다면 부담스럽다.  

스프링 3.0은 @Configuration을 이용한 자바 코드 DI 설정 방식을 지원하기 시작했지만, 이런 전용 태그를 대체할 자바 코드를 작성하기가 어렵기 때문에 XML을  
완전히 배제하기 힘들었지만 스프링 3.1에서는 특별한 목적을 위해 만들어진, 내부적으로 복잡한 로우 레벨의 빈을 등록해주는 전용 태그에 대응되는 애노테이션을 제공한다.  
<tx:annotation-driven />은 @EnableTransactionManagement를 붙여주는 것으로 간단히 변환할 수 있다. 스프링 3.1은 XML에서 자주 사용되는 전용  
태그를 @Enable로 시작하는 애노테이션으로 대체할 수 있게 다양한 애노테이션을 제공한다.

XML을 자바 코드로 전환했지만 막상 만들어진 설정용 자바 코드를 살펴보면 XML에 비해 더 간결해진 것도 없고 설정을 이해하는데 더 유리한 것도 없어보인다. 컴파일을  
할 때 오타를 잘 잡아주는 장점만 있다. 설정정보를 자바 코드를 이용해 작성할 때의 장점은 차차 알아보자

#### @Autowired를 이용한 자동와이어링
지금까지는 스프링 테스트 클래스나 DI 설정용 @Configuration 클래스에서 스프링 컨테이너가 생성한 빈을 클래의 멤버 필드로 주입받기 위해 @Autowired 애노테이션을  
사용했다. 빈으로 사용되는 UserServiceImpl이나 UserDaoJdbc 같은 클래스에서는 @Autowired를 사용할 수 없을까?

물론 사용할 수 있다. @Autowired는 자동와이어링 기법을 이용해서 조건에 맞는 빈을 찾아 자동으로 수정자 메소드나 필드에 넣어준다. 자동 와이어링을 이용하면  
컨테이너가 이름이나 타입을 기준으로 주입될 빈을 찾아주기 때문에 빈의 프로퍼티 설정을 직접해주는 자바 코드나 XML 양을 대폭 줄일 수 있다. 

방법은 간단하다 수정자 메소드 앞에 @Autowired를 붙여주면 된다. 만약 주입 가능한 빈타입이 2개 이상이라면 그 중에서 프로퍼티와 동일한 이름의 빈이 있는지 찾는다.  

다음은 sqlService 프로퍼티에 @Autowired를 적용해보자. 필드 위에 @Autowired 애노테이션을 붙여주면 된다. 설정 정보에서도 UserDao에서 수정자로 설정해주던  
설정들 없이 UserDao만 등록을 하면 sqlService는 자동으로 주입이 된다.

반면에 setDataSource() 수정자 메소드를 없애고 필드에 @Autowired를 적용하는 것은 불가능하다. 왜냐하면 setDataSource() 메소드는 여타 수정자 메소드처럼  
주어진 오브젝트를 그대로 필드에 저장하는 대신 JdbcTemplate을 생성해서 저장해주기 때문이다. 이러한 경우는 수정자를 남겨둬야한다.

#### @Component를 이용한 자동 빈 등록
@Component는 스프링이 애노테이션에 담긴 메타정보를 이용하기 시작했을 때 @Autowired와 함께 소개된 대표적인 애노테이션이다. @Component는 클래스에 부여된다.  
@Component가 붙은 클래스는 빈 스캐너를 통해 자동으로 빈으로 등록된다.

userDao() 메소드 자체를 제거해보자. userDao 빈을 프로퍼티로 사용하는 userService와 testUserService 빈을 위한 메소드에서 컴파일 에러가 난다.  
자동 등록이든 XML을 통한 등록이든, 아무튼 스프링 컨테이너에 등록된 빈을 가져와 사용할 때는 @Autowired로 가져오면 된다. 심지어 같은 클래스 안에 @Bean 메소드가  
존재하는 경우에도 메소드 호출 대신 @Autowired를 통해 빈을 참조할 수도 있다.

@Component 애노테이션이 달린 클래스를 자동으로 찾아서 빈을 등록해주게 하려면 빈 스캔 기능을 사용하겠다는 애노테이션 정의가 필요하다. 빈 자동등록이 컨테이너가  
디폴트로 제공하는 기능은 아니기 때문이다. 프로젝트 내의 모든 클래스패스를 다 뒤져서 @Component 애노테이션이 달린 클래스를 찾는 것은 부담이 많이 가는 작업이기  
때문에 특정 패키지 아래서만 찾도록 기준이 되는 패키지를 지정해줄 필요가 있다. 이때 사용되는 애노테이션은 @ComponentScan이다.  
@ComponentScan(basePackages="springbooks.user") 처럼 basePackage를 추가하면 되고, 여러개의 package를 추가할 수도 있다.

@Component가 붙은 클래스가 발견되면 새로운 빈을 자동으로 추가하고 빈의 클래스는 @Component가 붙은 클래스, 빈의 아이디는 따로 지정하지 않았으면 클래스 이름의  
첫 글자를 소문자로 바꿔서 사용한다. 만약 @Component가 붙은 클래스의 이름 대신 다름 이름을 빈의 아이디로 사용하고 싶다면 이름을 넣어주면 된다.  
@Component("userDao")

빈 자동등록에 @Component 애노테이션만 사용할 수 있는 것은 아니고 @Component 애노테이션을 메타 애노테이션으로 갖고 있는 애노테이션도 사용할 수 있다.  
그런데 스프링은 @Component 외의 애노테이션으로도 자동 빈 등록이 가능하게 해준다. 단지 빈 스캔 검색 대상으로 만드는 것 외에 부가적인 용도의 마커로 사용하게 하기  
위해서다. AOP에서 포인트컷을 작성할 때도 애노테이션을 사용할 수 있다. 애노테이션 포인트컷을 이용하면 패키지나 클래스 이름 패턴 대신 애노테이션을 기준으로 어드바이스  
적용 대상을 선별할 수 있다. AOP 적용 대상이 클래스 전체에 해당한다면 빈 자동등록용 애노테이션도 비슷한 용도로 사용하기 좋다.  
단지 AOP 포인트컷에 사용할 용도라면 각각 별개의 애노테이션을 만들면 되겠지만 빈 자동등록 대상임을 나타낼 수도 있어야 하니 뭔가 기준이 필요하다. 클래스라면 같은  
인터페이스를 구현하게 하거나 하나의 클래스를 상속하게 만들어서 여러 개의 클래스를 같은 그룹으로 묶을 수 있다. 그런데 애노테이션은 상속을 할 수 없고, 인터페이스를  
구현할 수도 없다.

여러 개의 애노테이션에 공통적인 속성을 부여하려면 메타 애노테이션을 이용한다. 메타 에노테이션은 애노테이션의 정의에 부여된 애노테이션을 말한다. 애노테이션이 빈 스캔을  
통해 자동등록 대상으로 인식되게 하려면 애노테이션 정의에 @Component를 메타 애노테이션으로 붙여주면 된다.  
SNS 서비스에 접속하는 기능을 제공하는 빈을 AOP 포인트컷으로 지정할 수 있도록 구분이 필요하다면 @SnsConnector라는 애노테이션을 하나 만들어 사용할 수 있을 것이다.  

```java
@Component
public @interface SnsConnector {
    
}
```

스프링은 DAO 빈을 자동등록 대상으로 만들 때 사용할 수 있게 @Repository 애노테이션을 제공한다. @Component를 부여하는 것만으로도 등록 대상으로 만드는데  
충분하지만 스프링은 DAO 기능을 제공하는 클래스에는 @Repository 애노테이션을 이용하도록 권장한다. @Repository는 @Component를 메타 애노테이션으로 갖고 있다.  

#### 컨텍스트 분리와 @Import
애플리케이션이 바르게 동작하는데 필요한 DI 정보와 테스트를 수행하기 위해 만든 DI 정보가 지금은 하나의 파일 안에 혼재해 있다. 이번에 할 작업은 성격이 다른 DI  
정보를 분리하는 것이다.

#### 테스트용 컨텍스트 분리
AppContext와 TestAppContext로 클래스를 분리하여 설정 정보를 나누자. 설정용 DI 클래스가 두 개가 됐다. 운영 시스템에서 애플리케이션이 동작할 때는  
AppContext만 참조하고 테스트에서는 AppContext와 TestAppContext 두 개의 DI 정보를 함께 사용하게 해주면 된다.  
테스트 코드에 넣은 DI 정보용 클래스도 수정할 필요가 있다. @ContextConfiguration(classes={TestAppContext.class, AppContext.class})처럼  
이 classes에 적용할 설정 클래스를 모두 나열해주면 된다.  

#### @Import
SQL 서비스는 그 자체로 독립적인 모듈처럼 취급하는 게 나아 보인다. SQL 서비스는 다른 애플리케이션에서도 사용될 수 있다. SQL 서비스와 관련된 빈들을 분리하기  
위해서 @Configuration 클래스를 하나 더 만들고 SqlServiceContext라는 이름으로 클래스를 만들어 SqlService 관련 빈을 옮겨보자. 

DI 설정정보를 담은 클래스가 세 개가 됐다. 두 개는 애플리케이션 핵심 빈 정보를 담고 있고, 하나는 테스트와 관련된 것만 갖고 있다. 설정 클래스가 추가됐으니, 또  
테스트 코드 @ContextConfiguration의 classes 내용을 수정해야 할까? 그래도 되지만 더 나은 방법이 있다.  
테스트용 설정정보는 어플리케이션 핵심 설정 정보와 깔끔하게 분리되는 편이 낫다. 반면에 SQL 서비스와 관련된 빈 설정은 별도 클래스로 분리하긴 했지만 애플리케이션이  
동작할 때 항상 필요한 정보다. 그래서 파일을 구분했더라도 애플리케이션 설정정보의 중심이 되는 AppContext와 긴밀하게 연결해주는 게 좋다. 

AppContext의 클래스 레벨에 @Import를 추가해서 SqlServiceContext를 가져오게 만들어보자.
```java
@Configuration
@EnableTransactionManagement
@ComponentScan(basePackages="springbook.user")
@Import(SqlServiceContext.class)
public class AppContext {
    
}
```

이제 AppContext가 설정 클래스로 사용되면 SqlServiceContext도 함께 적용될 것이다.
=