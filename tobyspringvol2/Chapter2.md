# 2장 데이터 엑세스 기술

## 2.1 공통 개념  

### DAO 패턴
데이터 액세스 계층은 DAO 패턴이라 불리는 방식응로 분리하는 것이 원칙이다. DAO 패턴은 DTO 또는 도메인 오브젝트만을 사용하는 인터페이스를 통해 데이터 액세스 기술을  
외부에 노출시키지 않도록 만드는 것이다. DAO 인터페이스는 기술과 상관없는 단순한 DTO나 도메인 모델만을 사용하기 때문에 언제든지 목오브젝트 같은 테스트 대역  
오브젝트로 대체해서 단위 테스트를 작성할 수 있다.

DAO 인터페이스와 DI  
DAO는 인터페이스를 이용해 접근하고 DI되도록 만들어야 한다. DAO 인터페이스에는 구체적인 데이터 엑세스 기술과 관련된 어떤 API나 정보도 노출하지 않는다.  
인터페이스를 만들 때 습관적으로 DAO 클래스의 모든 public 메소드를 추가하지 않도록 주의하자. DAO를 사용하는 서비스 계층에서 의미 있는 메소드만 인터페이스로  
공개해야 된다. 

예외처리  
데이터 엑세스 중에 발생하는 예외는 대부분 복구할 수 없다. 따라서 DAO 밖으로 던져질 때는 런타임 예외여야 한다. throws로 외부로 노출하지 말고 DAO 내부에서 발생하는  
예외는 모두 런타임 예외로 전환해야 한다.

#### 템플릿과 API
스프링은 DI의 응용 패턴인 템플릿/콜백 패턴을 통해 이런 판에 박힌 코드를 피하고 꼭 필요한 바뀌는 내용만 담을 수 있도록 데이터 엑세스 기술을 위한 템플릿을 제공한다.  

#### DataSource
JDBC를 통해 DB를 사용하려면 Connection 타입의 DB 연결 오브젝트가 필요하다. 매번 요청마다 Connection을 새롭게 만들면 비효율적이고 성능을 떨어뜨린다.  
그래서 보통 미리 정해진 개수만큼의 DB 커넥션을 풀에 준비해두고, 애플리케이션이 요청할 때마다 풀에서 꺼내 하나씩 할당해주고 다시 돌려받아서 풀에 넣는 식의 폴링 기법을  
이용한다.  
스프링에서는 DataSource를 하나의 독립된 빈으로 등록하도록 강력하게 권장한다. 

학습 테스트와 통합 테스트를 위한 DataSource  
- SimpleDriverDataSource  
스프링이 제공하는 가장 단순한 DataSource 구현 클래스다. getConnection()을 호출할 때마다 매번 DB 커넥션을 새로 만들고 따로 풀을 관리하지 않는다. 실전에서는  
  사용하면 안된다.
  

- SingleConnectionDataSource  
하나의 물리적인 DB 커넥션만 만들어두고 이를 계속 사용하는 DataSource다. 동시에 두 개 이상의 스레드가 동작하는 경우에는 하나의 커넥션을 공유하게 되므로 위험하다.  
  
오플소스 또는 상용 DB 커넥션 풀  
오픈소스로 개발된 DB 커넥션 풀도 많이 사용된다.

- 아파치 Commons DBCP  
가장 유명한 오픈소스 DB 커넥션 풀 라이브러리다.
  
- c3p0 JDBC/DataSource Resource Pool


JDNI/WAS DB 풀  
대부분의 자바 서버는 자체적으로 DB 풀 서비스를 제공해준다.


## 2.2 JDBC 
JDBC는 자바 데이터 엑세스 기술의 기본이 되는 로우레벨의 API다. JDBC는 표준 인터페이스를 제공하고 각 DB 벤더와 개발팀에서 이 인터페이스를 구현한 드라이버를 제공하는  
방식으로 사용된다. 

#### 스프링 JDBC 기술과 동작원리
스프링 3.0에서는 다섯 가지 종류의 접근 방법을 제공한다. JdbcTemplate은 그중에서 가장 오래된 기초적인 접근 방법이고 이제 사용할 필요가 없다.

스프링의 JDBC 접근 방법  
스프링 JDBCD의 접근 방법 중에서 가장 사용하기 편하고 자주 이용되는 것은 다음 두가지다.

- SimpleJdbcTemplate  
방대한 템플릿 메소드와 내장된 콜백을 제공한다. 
  
- SimpleJdbcInsert, SimpleJdbcCall  
DB가 제공해주는 메타정보를 활용해서 최소한의 코드만으로 단순한 JDBC 코드를 작성하게 해준다.
  
스프링 JDBC가 해주는 작업  
스프링 JDBC를 이용하면 다음과 같은 작업을 템플릿이나 스프링 JDBC가 제공하는 오브젝트에게 맡길 수 있다. 

- Connection 열기와 닫기  
스프링 JDBC를 사용하면 코드에서 직접 Connection을 열고 닫는 작업을 할 필요가 없다.  
  
- Statement 준비와 닫기  
SQL 정보가 담긴 Statement 또는 PreparedStatement를 생성하고 필요한 준비 작업을 해주는 것도 대부분 스프링 JDBC의 몫이다.
  
- Statement 실행  

- ResultSet 루프

- 예외처리와 변환  
체크 예외인 SQLException을 런타임 예외인 DataAccessException 타입으로 변환해준다.
  
- 트랜잭션 처리  

#### SimpleJdbcTemplate  
JdbcTemplate을 더욱 편리하게 사용할 수 있도록 기능을 향상시킨 것이다. 

SimpleJdbcTemplate 생성  
```java
SimpleJdbcTemplate template = new SimpleJdbcTemplate(dataSource);
```

DataSource는 보통 빈으로 등록해두므로 SimpleJdbcTemplate이 필요한 DAO에서 DataSource 빈을 DI 받아 SimpleJdbcTemplate을 생성해두고 사용하면  
된다.

SQL 파라미터  
SimpleJdbcTemplate에 작업을 요청할 때는 문자열로 된 SQL을 제공해줘야 한다. SQL에 매번 달라지는 값이 있는 경우에는 스트링 조합으로 SQL을 만들기보다는 "?"와  
같은 치환자를 넣어두고 파라미터 바인딩 방법을 사용하는 것이 편리하다.  
스프링 JDBC는 JDBC에서 제공하는 위치를 이용한 치환자인 "?"뿐 아니라 명시적으로 이름을 지정하는 이름 치환자도 지원한다.

```sql
INSERT INTO MEMBER(ID, NAME, POINT) VALUES(?, ?, ?);
INSERT INTO MEMBER(ID, NAME , POINT) VALUES(:id, :name, :point);
```

- MAP/MapSqlParameterSource  
Map이나 MapSqlParameterSource를 이용해 SQL에 바로 바인딩해줄 수 있다.
  
- BeanPropertySqlParameterSource  
맵 대신 도메인 오브젝트나 DTO를 사용하게 해준다. 오브젝트의 프로퍼티 이름과 SQL의 이름 치환자를 매핑해서 파라미터의 값을 넣어주는 방식이다.
  
SQL 실행 메소드  
INSERT, UPDATE, DELETE와 같은 SQL을 실행할 때는 SimpleJdbcTemplate의 update() 메소드를 사용한다.

- varargs  
위치 치환자(?)를 사용하는 경우 바인딩할 파라미터를 순서대로 전달하면 된다. 
  
- Map  
이름 치환자를 사용한다면 파라미터를 Map으로 전달할 수 있다. 
  
- SqlParameterSource  
도메인 오브젝트나 DTO를 이름 치환자에게 직접 바인딩하려면 SqlParameterSource 타입인 BeanPropertySqlParameterSource를 사용해 update()를 호출할  
  수 있다. 
  
SQL 조회 메소드  
- int queryForInt(String sql, [SQL 파라미터])  
하나의 int 타입값을 조회할 때 사용한다. 
  
- long queryForLong(String sql, [SQL 파라미터])  

- \<T> T queryForObject(String sql, Class\<T> requiredType, [SQL 파라미터])  
쿼리를 실행해서 하나의 값을 가져올 때 사용한다.
  
- \<T> T queryForObject(String sql, RowMapper\<T> rm, [SQL 파라미터])  
SQL 실행 결과, 하나의 로우가 돌아오는 경우에 사용한다. 단일 컬럼이 아니라 다중 컬럼을 가진 쿼리에 사용할 수 있다.
  
- \<T> List<T> query(String sql, RowMapper\<T> rm, [SQL 파라미터])  
SQL 실행 결과로 돌아온 여러 개의 컬럼을 가진 로우를 RowMapper 콜백을 이용해 도메인 오브젝트나 DTO에 매핑해준다.
  
- Map\<String, Object> queryForMap(String sql, [SQL 파라미터])  
단일 로우의 결과를 처리하는데 사용되며 RowMapper를 이용해 도메인 오브젝트나 DTO에 매핑하는 대신 맵에 로우의 내용을 저장해서 돌려준다.  
  
- List<Map\<String, Object>> queryForList(String sql, [SQL 파라미터])  
위의 다중 로우 버전이다. 
  
SQL 배치 메소드  
SQL 배치 메소드는 update()로 실행하는 SQL들을 배치 모드로 실행하게 해준다. 많은 SQL을 실행해야 하는 경우 배치 방식을 사용하면 DB 호출을 최소화할 수 있기  
때문에 성능이 향상될 수 있다. 동일한 SQL을 파라미터만 바꿔가면서 실행하는 경우에 사용할 수 있다.  

- int[] batchUpdate(String sql, Map\<String, ?>[] batchValues)  
이름 치환자를 가진 SQL과 파라미터 정보가 담긴 맵의 배열을 이용한다. 배열의 개수만큼 SQL을 실행해준다. 
  
- int[] batchUpdate(String sql, SqlParameterSource[] batchArgs)

- int[] batchUpdate(String sql, List<Object[]> batchArgs)

#### SimpleJDbcInsert  
DB의 메타정보를 활용해서 귀찮은 INSERT 문의 작성을 간편하게 만들어준다.

SimpleJdbcInsert 생성  
SimpleJdbcInsert는 테이블별로 만들어서 사용한다. 멀티스레드 환경에서 안전하니 변수로 두고 공유하는게 낫다. 
- SimpleJdbcInsert withTableName(String tableName)
- SimpleJdbcInsert withSchemaName(String schemaName)
- SimpleJdbcInsert withCatalogName(String catalogName)
- SimpleJdbcInsert usingColumns(String... columnNames)
- SimpleJdbcInsert usingGeneratedKeyColumns(String... columnNames)
- SimpleJdbcInsertOperations withoutTableColumnMetaDataAccess()

SimpleJdbcInsert 실행

- int execute([이름 치환자 SQL 파라미터])  
```sql
SimpleJdbcInsert insert = new SimpleJdbcInsert(dataSource).withTableName("member");
Member member = new Member(1, "Spring", 3.5);
insert.execute(new BeanPropertySqlParameterSource(member));
```

- Number executeAndReturnKet([이름 치환자 SQL 파라미터])  
- KeyHolder executeAndReturnKeyHolder([이름 치환자 SQL 파라미터])

#### SimpleJdbcCall

#### 스프링 JDBC DAO
스프링 JDBC를 이용해 DAO 클래스를 설계하는 방법을 알아보자. 보통 DAO는 도메인 오브젝트 또는 DB 테이블 단위로 만든다.  
가장 권장되는 DAO 작성 방법은 DAO는 DataSource에만 의존하게 만들고 스프링 JDBC 오브젝트는 코드를 이용해 직접 생성하거나 초기화해서 DAO의 인스턴스 변수에  
저장해두고 사용하는 것이다. SimpleJdbcTemplate을 생성하는 코드의 중복을 제거하기 위해서는 자체를 빈으로 등록해두고 주입받거나 추상 DAO를 만들고 이 클래스를  
상속하는 DAO들을 만드는 것이다. 


## 2.3 iBatis SqlMaps  
iBatis는 자바오브젝트와 SQL 문 사이의 자동 매핑 기능을 지원하는 ORM 프레임워크다. 코드 내에서 자바오브젝트만을 이용해 데이터 로직을 작성할 수 있게 해주고,  
SQL을 별도의 파일로 분리해서 관리하게 해주며, 오브젝트-SQL 사이의 파라미터 매핑 작업을 자동으로 해준다.  
SQL을 별도의 XML 파일 안에 작성하고 관리할 수 있기 때문에 변경이 있을 때 컴파일하지 않아도 된다. 

#### SqlMapClient 생성
iBatis의 핵심 API는 SqlMapClient 인터페이스에 담겨 있다. SqlMapClient는 SqlMapClientBuilder를 이용해 코드에서 생성할 수 있다. 하지만 스프링에서는  
SqlMapClient를 빈으로 등록해두고 DAO에서 DI 받아 사용해야 하기 때문에 SQLMapClient를 빈으로 등록해주는 팩토리 빈의 도움이 필요하다.  
SqlMapClientFactoryBean을 이용해서 SqlMapClient를 빈으로 등록할 수 있다. 

iBatis 설정파일과 매핑파일  
보통 하나의 설정파일과 한 개 이상의 매핑파일로 구성된다.

- 설정파일  
설정파일에는 데이터소스, 트랜잭션 매니저, 매핑 리소스 파일 목록, 프로퍼티, 타입별칭과 핸들러, 오브젝트 팩토리와 설정 프로퍼티 값을 넣을 수 있다. 
```xml
<sqlMapConfig>
  <sqlMap resource="springbook/learningtest/spring/ibatis/Member.xml"/>
</sqlMapConfig>
```
DB 커넥션과 트랜잭션 관리를 위한 정보는 설정파일에 넣지 않고 스프링 빈으로 등록한 것을 사용하는 게 바람직하다.

- 매핑파일  
```xml
<sqlMap namespace="Member">
  <typeAlias alias="Member" type="springbook.learningtest.spring.jdbc.Memeber"/> #파라미터와 결과 매핑에 사용할 클래스 별칭 등록
  
  <delete id="deleteMemberAll">
    delete from member
  </delete>
  
  <insert id="insertMember" parameterCalss="Member">
    insert into member (id, name, point) values(#id#, #name#, #point#)
  </insert>
</sqlMap>
```

#### SqlMapClientTemplate
DAO에서 SqlMapClient를 직접 사용하는 대신 스프링이 제공하는 템플릿 오브젝트인 SqlMapClientTemplate을 이용하는 것이 좋다. 그래야먄 스프링 데이터 엑세스  
기술이 제공하는 다양한 혜택을 받을 수 있기 때문이다. 템플릿을 사용하면 예외 변환, 스프링 트랜잭션과 동기화 등이 지원된다.

```java
public class MemberDao {
    private SqlMapClientTemplate sqlMapClientTemplate;
    
    public void setSqlMapClient(SqlMapClient sqlMapClient) {
        sqlMapClientTemplate = new SqlMapClientTemplate(sqlMapClient);
    }
}
```

스프링이 제공하는 SqlMapClientDaoSupport를 상속해 DAO를 만들어도 된다. 템플릿 오브젝트는 getSqlMapClientTemplate() 메소드로 가져올 수 있다.

등록, 수정, 삭제  
- insert()  
```java
Object insert(String statementName)
Object insert(String statementName, Object parameterObject)
```

- update()  
```java
int uddate(String statementName)
int uddate(String statementName, Object parameterObject)
int uddate(String statementName, Object parameterObject, int requiredRowsAffected) //영향 받은 로우 개수 체크
```

- delete()  
```java
int delete(String statementName)
int delete(String statementName, Object parameterObject)
int delete(String statementName, Object parameterObject, int requiredRowsAffected) //영향 받은 로우 개수 체크
```

조회  
- 단일 로우 조회: queryForObject()  
```java
Object queryForObject(String statementName)
Object queryForObject(String statementName, Object parameterObject)
Object queryForObject(String statementName, Object parameterObject, Object resultObject) // 결과를 매핑해서 돌려줄 오브젝트 제공
```

- 다중 로우 조회: queryForList()
```java
List queryForList(String statementName)
List queryForList(String statementName, Object parameterObject)
List queryForList(String statementName, int skipResults, int maxResults)
List queryForList(String statementName, Object parameterObject, int skipResults, int maxResults)
```

- 다중 로우 조회: queryForMap()
- 다중 로우 조회: queryWithRowHandler()

#### SqlMapClientCallback
스프링이 제공해주는 콜백이 내장된 템플리 메소드를 이용하는 대신 직접 iBatis의 SqlMapExecutor API를 사용하고 싶다면 SqlMapClientCallback 인터페이스를  
사용할 수 있다. 


## 2.4 JPA
JPA는 Java Persistent API의 약자로 JavaEE와 JavaSE를 위한 영속성관리와 O/R 매핑을 위한 표준 기술이다.  
근본적으로 RDB와 자바오브젝트의 성격이 다르기 때문에 발생하는 많은 불일치가 있고, 이를 코드에서 일일이 다뤄줘야 하기 때문에 생산성과 품질 면에서 손해를 보기 쉽다.  

#### EntityManagerFactory 등록 
JPA 퍼시스턴스 컨텍스트에 접근하고 엔티티 인스턴스를 관리하려면 JPA의 핵심 인터페이스인 EntityManager를 구현한 오브젝트가 필요하다. 애플리케이션이 관리하는  
EntityManager, 컨테이너가 관리하는 EntityManager 두 가지 방식으로 관리된다.  
어떤 방식을 사용하든 반드시 EntityManagerFactory를 빈으로 등록해야 된다.

LocalEntityManagerFactoryBean  
스프링의 빈으로 등록한 DataSource를 사용할 수 없다는 점에서 제약이 있다.

JavaEE5 서버가 제공하는 EntityManagerFactory  

LocalContainerEntityManagerFactoryBean  
스프링이 직접 제공하는 컨테이너 관리 EntityManager를 위한 EntityManagerFactory를 만들어준다. 이 빈은 META-INF/persistence.xml을 참고해서  
퍼시스턴스 유닛과 이를 활용하는 EntityManagerFactory를 만든다.

#### EntityManager와 JpaTemplate
스프링에서는 템플릿 방식의 JpaTemplate뿐 아니라 JPA API를 직접 사용해 DAO를 작성할 수도 있다. JPA의 핵심 프로그래밍 인터페이스는 EntityManager다.  
JPA DAO에서 EntityManager를 사용하는 네 가지 방법을 알아보자.  

JdbcTemplate  
실제로 스프링에서 JPA를 사용해 DAO를 작성할 때 이 JpaTemplate은 자주 사용되지 않는다.  
```java
public class MemberTemplateDao {
    private JpaTemplate jpaTemplate;
    
    @Autowired
    public void init(EntityManagerFactory emf) {
        jpaTemplate = new JpaTemplate(emf);
    }
}
```

JpaDaoSupport 클래스를 상속해서 DAO를 만들면 getJpaTemplate() 메소드를 이용해 jpaTemplate을 가져올 수 있다.   
JpaTemplate을 사용할 때는 기본적으로 JpaCallback 인터페이스의 doInJpa() 메소드에 필요한 작업을 넣는다.

```java
List<Member> ms = templateDao.jpaTemplate.execute(new JpaCallback<List<Member>>() {
    public List<Member> doInJpa(EntityManager entityManager) throws PersistanceException {
        return entityManager.createQuery("selet m from Member m").getResultList();
        }
        })
```

JpaTemplate은 콜백 오브젝트 없이도 간단한 메소드를 이용해서 EntityManager가 제공하는 대부분의 기능을 사용하게 해준다.  
```java
Member m = new Member(1, "Spring", 8.9);
jpaTemplate.persis(m);
Member m2 = templateDao.jpaTemplate.find(Member.clss, 1);
```

#### 애플리케이션 관리 EntityManager와 @PersistenceUnit  
EntityManager를 사용하는 두 번째 방법은 컨테이너 대신 애플리케이션 코드가 관리하는 EntityManager를 이용하는 것이다.  
EntityManager는 EntityManagerFactory가 있다면 다음과 같이 직접 생성할 수 있다.  
```java
EntityManager em = entityManagerFactory.createEntityManger();
```

다만 컨테이너가 관리하지 않는 EntityManager이므로 트랜잭션은 직접 시작하고 종료해야 한다.
```java
em.getTransaction().begin();
em.getTransaction().commit();
```

DAO 클래스에 EntityManagerFactory를 DI 하는 방법은 두 가지가 있다.  
- @Autowired, @Resource  
- @PersistenceUnit  
스프링의 DI 방식 대신 JPA 표준 스펙에 나온 방식을 이용하는 것이다. 이때 사용되는 애노테이션은 javax.persistence 패키지의 @PersistenceUnit이다.
  
```java
public class MemberDao {
    @PersistanceUnit EntityManagerFactory emf;
}
```

이렇게 만들어진 DAO 코드가 @Autowired를 이용했을 때와 다른 점은 스프링 프레임워크에 의존도가 전혀 없는 순수한 JPA 코드라는 것이다. 그런데 이 방법은 자주  
쓰이지는 않는다.

컨테이너 관리 EntityManager와 @PersistenceContext  
EntityManager를 사용해 JPA 코드를 작성하는 가장 대표적인 방법은 컨테이너가 제공하는 EntityManager를 직접 제공받아서 사용하는 것이다. JPA의  
@PersistenceContext 애노테이션을 사용하면 된다.

```java
public class MemberDao {
    @PersistenceContext EntityManager em;
    
    public void addMember(Member member) {
        em.persist(member);
    }
}
```

EntityManger는 그 자체로 멀티스레드에서 공유해서 사용할 수 없다. 사용자의 요청에 따라 만들어지는 스레드별로 독립적인 EntityManager가 만들어져 사용돼야 한다.  
이렇게 인스턴스 변수에 한 번 DI 받아놓고 같은 오브젝트를 여러 스레드가 동시에 사용할 수 있는 이유는 @PersistenceContext로 주입되는 EntityManger는 실제가  
아니라 현재 진행 중인 트랜잭션에 연결되는 퍼시스턴스 컨텍스트를 갖는 일종의 프록시이기 때문이다. 컨테이너가 관리하는 EntityManger는 스코프 빈과 비슷한 방식으로  
동작한다. 실제 EntityManager 오브젝트는 현재 스레드와 연결된 트랜잭션에 따라서 독립적으로 만들어지고 그 범위 안에서 존재하다가 제거된다.

@PersistenceContext와 확장된 퍼시스턴스 컨텍스트  
@PersistenceContext 애노테이션을 별도의 설정 없이 사용하면 디퐅트 값인 PersistenceContextType.TRANSACTION이 적용되면서 트랜잭션 스코프의  
퍼시스턴스 컨텍스트로 EntityManager가 만들어지고 관리된다고 했다. 마지막 방법은 이 type을 PersistenceContextType.EXTENDED로 지정하는 것이다.  
이렇게 하면 트랜잭션 스코프 대신 확장된 스코프를 갖는 EntityManager가 만들어진다. JPA에서 이 확장된 퍼시스턴스 컨텍스트는 상태유지 세션빈에 바인딩되는 것을  
말한다.  
상태유지 세션빈은 사용자별로 독립적이며 장기간 보존되는 오브젝트로 만들어진다. 이 세션빈에 바인딩되는 EntityManager 역시 사용자별로 독립적으로 만들어지고 장기간  
보존된다. 따라서 이 방식은 스프링의 싱글톤에는 사용할 수 없고 상태를 가진 세션빈이나 장기간 지속되는 스코프 빈에만 사용될 수 있다. 

JPA 예외 변환 AOP  
DAO의 메소드에 적용되는 AOP를 이용해서 JAP API가 던지는 JPA 예외를 스프링의 예외로 전환해주는 부가기능을 추가해줄 수 있다.  
- @Repository 
먼저 예외 변환이 필요한 DAO 클래스에 @Repository 애노테이션을 부여한다. @Repository가 붙은 DAO 클래스의 메소드는 AOP를 이용한 예외 변환 기능이 부가될  
  빈으로 선정된다.
  
- PersistenceExceptionTranslationPostProcessor  
다음은 @Repository 애노테이션이 붙은 빈을 찾아서 예외 변환 기능을 가진 AOP 어드바이스를 적용해주는 후처리기가 필요하다. 간단히 빈으로 등록해주기만 하면 된다.  
  
JPA는 데이터 엑스스 프로그래밍의 패러다임이 다르기 때문에 JDBC를 사용했을 때처럼 다양한 DataAccessException의 서브클래스로 매핑돼서 던져지리라고 기대할 수  
없다. 


## 2.5 하이버네이트 
하이버네이트의 창시자인 개빈 킹은 하이버네이트의 경험을 바탕으로 표준 ORM 기술인 JPA 스펙의 초안을 작성하고 전문가 그룹에 참여해서 지금까지 JPA 표준의 발전에  
많은 영향을 미쳐오고 있다. 하이버네이트는 그 자체로 독립적인 API와 기능을 가진 ORM 제품이면서 동시에 JPA의 핵심 구현 제품이기도 하다. 이 절에서는 JPA 구현  
엔진으로서가 아니라 하이버네이트 자체 API를 사용하는 DAO를 작성하는 방법을 다룬다.  


## 2.6 트랜잭션 
선언적 트랜잭션 경계설정 기능을 이용하면 코드 내에서 직접 트랜잭션을 관리하고 트랜잭션 정보를 파라미터로 넘겨서 사용하지 않아도 된다. 트랜잭션이 시작되고 종료되는  
지점은 별도의 설정을 통해 결정된다. 선언적 트랜잭션 경계설절을 사용하면, 결국 코드의 중복을 제거하고 작은 단위의 컴포넌트로 쪼개서 개발한 후에 이를 조합해서 쓸 수  
있다.  
EJB의 이런 선언적 트랜잭션 기능을 복잡한 환경이나 구현조건 없이 평범한 POJO로 만든 코드에 적용하게 해주는 것이 바로 스프링이다. 

#### 트랜잭션 추상화와 동기화
스프링이 제공하는 트랜잭션 서비스는 트랜잭션 추상화와 트랜잭션 동기화 두 가지로 생각해볼 수 있다.  
스프링은 데이터 액세스 기술과 트랜잭션 서비스 사이의 종속성을 제거하고 스프링이 제공하는 트랜잭션 추상 계층을 이용해서 트랜잭션 기능을 활용하도록 만들어준다. 이를  
통해 트랜잭션 서비스의 종류나 환경이 바뀌더라도 트랜잭션을 사용하는 코드는 그대로 유지할 수 있는 유연성을 얻을 수 있다.  
스프링의 트랜잭션 동기화는 트랜잭션을 일정 범위 안에서 유지해주고, 어디서든 자유롭게 접근할 수 있게 만들어준다.

PlatformTransactionManager  
스프링 트랜잭션 추상화의 핵심 인터페이스는 PlatformTransactionManager이다. 모든 스프링의 트랜잭션 기능과 코드는 이 인터페이스를 통해서 로우레벨의 트랜잭션  
서비스를 이용할 수 있다.  
```java
public interface PlatformTransactionManager {
    TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException;
    void commit(TransactionStatus status) throws TransactionException;
    void rollback(TransactionStatus status) throws TransactionException;
}
```

PlatformTransactionManager는 트랜잭션 경계를 지정하는 데 사용한다. 트랜잭션을 시작한다는 의미의 begin()과 같은 메소드 대신 적절한 트랜잭션을 가져온다는  
의미의 getTransaction() 메소드를 사용한다. getTransaction()은 트랜잭션 속성에 따라서 새로 시작하거나 진행 중인 트랜잭션에 참여하거나, 진행 중인  
트랜잭션을 무시하고 새로운 트랜잭션을 만드는 식으로 상황에 따라 다르게 동작한다.  
TransactionDefinition은 트랜잭션의 네 가지 속성을 나타내는 인터페이스다. TransactionStatus는 현재 참여하고 있는 트랜잭션의 ID와 구분정보를 담고 있다.  
커밋 또는 롤백 시에 이 TransactionStatus를 사용한다.

트랜잭션 매니저의 종류
스프링이 제공하는 PlatformTransactionManager 구현 클래스를 살펴보자. 

- DataSourceTransactionManager  
Connection의 트랜잭션 API를 이용해서 트랜잭션을 관리해주는 트랜잭션 매니저다. 이 트랜잭션 매니저를 사용하려면 트랜잭션을 적용할 DataSource가 스프링의 빈으로  
  등록돼야 한다. 
  
- JpaTransactionManager  
JPA를 이용하는 DAO에는 JpaTransactionManager를 사용한다.
  
- HibernateTransactionManager  
- JmsTransactionManager, CciTransactionManager  
- JtaTransactionManager

#### 트랜잭션 경계설정 전략
트랜잭션 경계를 설정하는 방법은 코드에 의한 프로그램적인 방법과, AOP를 이용한 선언적인 방법으로 구분할 수 있다. 전자는 트랜잭션을 다루는 코드를 직접 만들고, 후자는  
AOP를 이용해 기존 코드에 트랜잭션 경계설정 기능을 부여해준다.  

코드에 의한 트랜잭션 경계설정  
스프링의 트랜잭션 매니저는 모두 PlatformTransactionManager를 구현하고 있다. 따라서 이 인터페이스로 현재 등록되어 있는 트랜잭션 매니저 빈을 가져올 수 있다면  
트랜잭션 매니저 종류에 상관없이 동일한 방식으로 트랜잭션을 제어하는 코드를 만들 수 있다. PlatformTransactionManager를 직접 사용하면 try/catch문을  
사용해야하기 때문에 직접 사용하는 대신 템플릿/콜백 방식의 TransactionTemplate를 이용하면 편리하다.  
코드에 의한 트랜잭션은 실제로는 많이 사용되지 않고 테스트에서 의도적으로 필요로 할 때 주로 사용한다.  

선언적 트랜잭션 경계설정  
선언적 트랜잭션을 이용하면 코드에는 전혀 영향을 주지 않으면서 특정 메소드 실행 전후에 트랜잭션이 시작되고 종료되거나 기존 트랜잭션에 참여하도록 만들 수 있다.  
이를 위해서는 데코레이터 패턴을 적용한 트랜잭션 프록시 빈을 사용해야 한다.  
트랜잭션은 다음과 같은 두가지 방법이 가장 많이 사용된다.

1. aop와 tx 네임스페이스

스프링은 AOP 기능과 트랜잭션 설정을 위해 편리하게 사용할 수 있는 전용 태그를 제공한다. aop 스키마의 태그와 tx 스키마의 태그를 사용할 수 있다.  
어떤 부가기능을 사용할지 결정하는 어드바이스가 있어야하고 어드바이스를 적용할 대상을 선정하는 포인트컷이 필요하다. 트랜잭션 어드바이스와 포인트컷을 결합해서 하나의  
AOP 모듈을 정의한다. 이 모듈을 스프링에서는 어드바이저라고 부른다.  
트랜잭션을 적용하려면 트랜잭션 매니저가 필요하다. 
```xml
<tx:advicce id="txAdvice" transaction-manager="transactionManager">
  <tx:attributes>
    <tx:method name="*" />
  </tx:attributes>
</tx:advicce>
```

어드바이스가 준비됐으니 다음은 포인트컷을 정의할 차례다. 포인트컷은 AspectJ 표현식을 사용하는 것이 가장 편하다. aop 스키마의 pointcut 태그를 사용하면 된다.  
```xml
<aop:pointcut id="txPointcut" expression="execution(* *..MemberDao.*(..))"/>
```

포인트컷은 기본적으로 인터페이스에 적용된다. 스프링 AOP의 동작 원리인 JDK 다이내믹 프록시는 인터페이스를 이용해 프록시를 만들기 때문이다.  
다음은 어드바이저를 정의할 차례다.  
```xml
<aop:config>
  <aop:pointcut id="txPointcut" expression="execution(* *..MemberDao.*(..))"/>
  <aop:advisor advice-ref="txAdvice" pointcut-ref="txPointcut"/>
</aop:config>
```

2. @Transactional

명시적인 방법에서는 설정파일에 명시적으로 포인트컷과 어드바이스를 정의하지 않는다. `<tx:annotation-driven/>` 한줄만 추가하면 된다.  
메소드에 @Transactional이 있으면 클래스 레벨의 @Transactional 선언보다 우선해서 적용된다.  
@Transactional을 적용하는 우선순위는 클래스의 메소드, 클래스, 인터페이스의 메소드, 인터페이스 순이다.

세밀한 설정이 필요한 경우는 aop와 tx를 사용하면 xml이 복잡해지는 단점이 있으나 @Transactional을 사용할 경우는 코드가 지저분해질 수 있다는 단점이 있다.

프록시모드:인터페이스와 클래스  
다이나믹 프록시를 사용하려면 인터페이스가 있어야한다. 인터페이스를 구현하지 않은 클래스의 경우에는 CGLib 라이브러리가 제공해주는 클래스 레벨의 프록시도 사용할 수 있다.  
클래스 프록시는 코드를 함부로 손댈 수 없는 레거시 코드나, 여타 제한 때문에 인터페이스를 사용하지 못했을 경우에만 사용해야 한다. 

AOP 방식: 프록시와 AspectJ  
스프링의 AOP는 기본적으로 프록시 방식이다. 인터페이스를 이용하는 JDK 다이내믹 프록시든 클래스에 바로 프록시를 만드는 CGLib이든, 모두 프록시 오브젝트를 타깃  
오브젝트 앞에 두고 호출 과정을 가로채서 트랜잭션과 같은 부가적인 작업을 진행해준다.  
스프링의 프록시 AOP 대신 AOP 전용 프레임워크인 AspectJ의 AOP를 사용할 수 있다. AspectJ AOP는 스프링과 달리 프록시 타깃을 오브젝트 앞에 두지 않고 타깃  
오브젝트 자체를 조작해서 부가기능을 직접 넣는 방식이다.  
타깃 오브젝트 내에서 호출이 일어난다면 프록시가 제대로 동작하지 않는다. 이미 프록시를 통해 타겟오브젝트에 들어온 상태이기 때문에 다시 프록시를 거치지 않기 때문이다.  
이 문제를 해결하기 위해서는 두가지 방법을 고려할 수 있다.
- AopContext.currentProxy()  
이 방법을 사용하면 타겟 오브젝트에서 다시 프록시를 거쳐 타깃 오브젝트의 메소드를 호출할 수 있다.
  
- AspectJ AOP  
프록시 대신 클래스 바이트 코드를 직접 변경해서 부가 기능을 추가하기 때문에 트랜잭션 부가기능이 잘 동작한다.
  
#### 2.6.3 트랜잭션 속성
트랜잭션 전파: propagation  
트랜잭션을 시작하거나 기존 트랜잭션에 참여하는 방법을 결정하는 속성이다. 
- REQUIRED(default)  
미리시작된 트랜잭션이 있으면 참여하고 없으면 새로 시작한다.
  
- SUPPORTS  
이미 시작된 트랜잭션이 있으면 참여하고 없으면 트랜잭션이 없이 진행한다.
  
- MANDATORY  
트랜잭션이 시작된 것이 없으면 새로 시작하는 대신 예외를 발생시킨다. 혼자서는 독립적으로 트랜잭션을 진행하면 안되는 경우에 사용한다. 
  
- REQUIRED_NEW  
항상 새로운 트랜잭션을 시작한다. 이미 진행 중인 트랜잭션이 있으면 트랜잭션을 잠시 보류시킨다.
  
- NOT_SUPPORTED  
이미 진행 중인 트랜잭션이 있으면 보류시킨다.
  
- NEVER  
트랜잭션을 사용하지 않도록 강제한다. 
  
- NESTED  
이미 진행중인 트랜잭션이 있으면 중첩 트랜잭션을 시작한다. 메인 트랜잭션이 롤백되면 중첩된 로그 트랜잭션도 같이 롤백되지만, 반대로 중첩된 로그 트랜잭션이 롤백돼도  
  메인 작업에 이상이 없다면 메인 트랜잭션은 정상적으로 커밋된다.
  
트랜잭션 격리수준: isolation  
트랜잭션 격리수준은 동시에 여러 트랜잭션이 진행될 때에 트랜잭션의 작업 결과를 여타 트랜잭션에게 어떻게 노출할 것인지를 결정하는 기준이다.  

- DEFAULT  
사용하는 데이터 액세스 기술 또는 DB 드라이버의 디폴트 설정을 따른다.  
  
- READ_UNCOMMITTED  
가장 낮은 격리 수준이다. 하나의 트랜잭션이 커밋되기 전에 그 변화가 다른 트랜잭션에 그대로 노출되는 문제가 있다. 가장 빠르기 때문에 일관성이 조금 떨어지더라도  
  성능을 극대화할 때 의도적으로 사용하기도 한다.
  
- READ_COMMITTED  
가장 많이 사용되는 격리수준이다. 다른 트랜잭션이 커밋하지 않은 정보는 읽을 수 없다. 대신 하나의 트랜잭션이 읽은 로우를 다른 트랜잭션이 수정할 수 있다. 
  
- REPEATABLE_READ  
하나의 트랜잭션이 읽은 로우를 다른 트랜잭션이 수정하는 것을 막아준다. 하지만 새로운 로우를 추가하는 것은 제한하지 않는다. 
  
- SERIALIZABLE  
트랜잭션을 순차적으로 진행시켜 주기 때문에 여러 트랜잭션이 동시에 같은 테이블의 정보를 액세스하지 못한다.
  
트랜잭션 제한시간: timeout  
트랜잭션에 제한시간을 초단위로 지정할 수 있다.  

읽기전용 트랜잭션: read-only, readOnly  
성능을 최적화하기 위해 사용할 수도 있고, 특정 트랜잭션 작업 안에서 쓰기 작업이 일어나는 것을 의도적으로 방지하기 위해 사용할 수도 있다. 

트랜잭션 롤백 예외: rollback-for, rollbackFor, rollbackForClassName  
런타임 예외만 롤백 대상으로 삼는다. 체크 예외지만 롤백 대상으로 삼아야하는 것이 있다면 위의 값에 넣어주면 된다.

트랜잭션 커밋 예외: no-rollback-for, noRollbackFor, noRollbackForClassName  
기본적으로는 롤백 대상인 런타임 예외를 트랜잭션 커밋 대상으로 지정해준다.

보통은 read-only 속성정도만 사용하고 나머지는 디폴트로 지정하는 경우가 많다. 세밀한 속성은 DB나 WAS의 트랜잭션 매지너의 설정을 이용해도 되기 때문이다.  
세밀한 트랜잭션 속성 지정이 필요한 경우에는 @Transactional을 사용하는 편이 좋으나 대신 트랜잭션 속성이 전체적으로 어떻게 지정되어 있는지 한눈에 보기 힘들다는  
단점이 있다. 그래서 사전에 트랜잭션 속성에 관한 정책이나 가이드라인을 잘 만들어둬야 한다.  

#### 2.6.4 데이터 엑세스 기술 트랜잭션의 통합  
두 가지 이상의 데이터 액세스 기술을 하나의 DAO에서 하나의 트랜잭션으로 사용하고 싶을 수 있다. 스프링은 두 개 이상의 데이터 액세스 기술로 만든 DAO를 하나의  
트랜잭션으로 묶어서 사용하는 방법을 제공한다.


## 2.7 스프링 3.1의 데이터 액세스 기술
스프링의 서브 프로젝트인 스프링의 데이터 프로젝트는 여러 기술들을 어플리케이션에서 손쉽게 사용할 수 있게 도와주는 모듈을 제공한다. 

### 2.7.1 persistence.xml 없이 JPA 사용하기  
JPA에서는 META-INFO/presistence.xml 파일을 이용해 퍼시스턴트 유닛을 정의하고, 이를 이용해 EntityFactoryManager를 생성한다. 스프링은  
LocalContainerEntityManagerFactoryBean 등을 이용해서 persistence.xml에 넣어야할 DB 연결정보나 트랜잭션 광련 정보를 생략하고, 관련된 스프링의  
빈을 대신 사용할 수 있게 해준다.  
스프링 3.1에서는 스프링의 클래스 스캐닝 기술을 이용해 JPA 엔터티 클래스를 찾아 자동으로 등록하는 방법이 지원된다. 이를 적용하면 persistence.xml 파일을  
아예 생략할 수도 있다.  
사용 방법은 JPA 엔터티 클래스가 담긴 패키지를 LocalContainerEntityManagerFactoryBean 빈의 packagesToScan 프로퍼티에 넣어주면 된다.

### 2.7.3 @EnableTransactionManager
XML의 `<tx:annotation-driven/>`과 동일한 컨테이너 인프라 빈을 등록해주는 자바 코드 설정용 애노테이션이다. @Transactional을 이용한 트랜잭션 설정을  
가능하게 해준다.