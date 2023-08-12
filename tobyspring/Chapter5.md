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

