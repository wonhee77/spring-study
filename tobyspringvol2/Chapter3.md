# 3장 스프링 웹 기술과 스프링 MVC
 
## 3.1 스프링의 웹 프레젠테이션 계층 기술
스프링은 의도적으로 서블릿 웹 애플리케이션의 컨텍스트를 두 가지로 분리해놓았다. 웹 기술에서 완전히 독립적인 비즈니스 서비스 계층과 데이터 액세스 계층을 담은  
루트 애플리케이션 컨텍스트와 스프링 웹 기술을 기반으로 동작하는 웹 관련 빈을 담은 서블릿 애플리케이션 컨텍스트다. 이렇게 스프링 컨텍스트를 두 가지로 분리해둔 이유는  
스프링 웹 서블릿 컨텍스트를 통째로 다른 기술로 대체할 수 있도록 하기 위해서이다.

### 3.1.1 스프링에서 사용되는 웹 프레임워크의 종류

`스프링 서블릿/스프링 MVC`  
스프링이 직접 제공하는 서블릿 기반의 MVC 프레임워크다. 프론트 컨트롤러 역할을 하는 DispatcherServlet을 핵심 엔진으로 사용한다.

`스프링 포틀릿`  
스프링이 제공하는 포틀릿 MVC 프레임워크다.

DispatcherServlet과 MVC 아키텍처  
스프링의 웹 기술은 MVC 아키텍처를 근간으로 하고 있다. MCV는 프레젠테이션 계층의 구성요소의 정보를 담은 모델(M), 화면 출력 로직을 담은 뷰(V), 그리고 제어 로직을  
담은 컨트롤러(C)로 분리하고 이 세 가지 요소가 서로 협력해서 하나의 웹 요청을 처리하고 응답을 만들어 내는 구조이다.  
MVC 아키텍처는 보통 프론트 컨트롤러 패턴과 함께 사용된다. 프론트 컨트롤러 패턴은 중앙 집중형 컨트롤러를 프레젠테이션 계층의 제일 앞에 둬서 서버로 들어오는 모든  
요청을 먼저 받아서 처리하게 만든다. 프론트 컨트롤러는 클라이언트가 보낸 요청을 받아서 공통적인 작업을 먼저 수행한 후에 적절한 세부 컨트롤러로 작업을 위임해주고,  
클라이언트에게 보낼 뷰를 선택해서 최종 결과를 생성하는 등의 작업을 수행한다. 프론트 컨트롤러는 컨트롤러와 뷰, 그리고 그 사이에서 주고받는 모델, 세 가지를 이용해서  
작업을 수행하는게 일반적이다.  
스프링이 제공하는 스프링 서블릿/MVC의 핵심이 DispatcherServlet이라는 프론트 컨트롤러다.  
서버가 브라우저나 여타 HTTP 클라이언트로부터 HTTP 요청을 받기 시작해서 다시 HTTP로 결과를 응답해주기까지의 과정을 살펴보자. 

1. DispatcherServlet의 HTTP 요청 접수  
자바 서버의 서블릿 컨테이너는 HTTP 프로토콜을 통해 들어오는 요청이 스프링의 DispatcherServlet에 할당된 것이라면 HTTP 요청정보를 DispatcherServlet에  
   전달해준다. web.xml에는 DispatcherServlet이 전달 받을 URL의 패턴이 정의되어 있다.  
   ```xml
   <servlet-mapping>
      <servlet-name>Spring MVC Dispatcher Servlet</servlet-name>
      <url-pattern>/app/*</url-pattern>
   </servlet-mapping>
   ```
   
2. DispatcherSevlet에서 컨트롤러로 HTTP 요청 위임  
DispatcherServlet은 URL이나 파라미터 정보, HTTP 명령 등을 참고로 해서 어떤 컨트롤러에게 작업을 위임할지 결정한다. 컨트롤러를 선정하는 것은 Dispatcher  
   Servlet의 핸들러 매핑 전략을 이용한다. 스프링에서는 컨트롤러를 핸들러라고도 부른다. 사용자 요청을 기준으로 어떤 핸들러에게 작업을 위임할지를 결정해주는 것을  
   핸들러 매핑 전략이라고 한다.  
   이를 전략이라고 부르는 이유는 DI의 가장 대표적인 용도로 할 수 있는 전략 패턴이 적용되어 있기 때문이다.  
   DispatcherServlet은 오브젝트 어댑터 패턴을 사용해서 특정 컨트롤러를 호출해야 할 때는 해당 컨트롤러 타입을 지원하는 어댑터를 중간에 껴서 호출하는 것이다.  
   하나의 DispatcherServlet이 동시에 여러 가지 타입의 컨트롤러를 사용할 수 있다. DI를 통해 자유롭게 확장이 가능하다.  
   DispatcherServlet이 핸들러 어댑터에 웹 요청을 전달할 때는 모든 웹 요청 정보가 담긴 HttpServletRequest 타입의 오브젝트를 전달해준다. 이를 어댑터가  
   적절히 변환해서 컨트롤러의 메소드가 받을 수 있는 파라미터로 변환해서 전달해주는 것이다.
   
3. 컨트롤러의 모델 생성과 정보 등록  
MVC 패턴의 장점은 정보를 담고 있는 모델과 정보를 어떻게 뿌려줄지를 알고 있는 뷰가 분리된다는 점이다.  
   컨트롤러의 작업은 먼저 사용자 요청을 해석하는 것, 그에 따라 실제 비즈니스 로직을 수행하도록 서비스 계층 오브젝트에게 작업을 위임하는 것, 그리고 결과를 받아서  
   모델을 생성하는 것, 마지막으로 어떤 뷰를 사용할지 결정하는 것의 네 가지로 분류할 수 있다.
   
4. 컨트롤러의 결과 리턴: 모델과 뷰  
컨트롤로가 뷰 오브젝트를 직접 리턴할 수도 있지만, 보통은 논리적인 이름을 리턴해주면 DispatcherServlet의 전략인 뷰 리졸버가 이를 이용해 뷰 오브젝트를 생성해준다.  
   모델과 뷰를 넘기는 것으로 컨트롤러의 책임은 끝이다. 다시 작업은 DispatcherServlet으로 넘어간다.
   
5. DispatcherServlet의 뷰 호출과 6. 모델 참조  
DispatcherServlet이 컨트롤러로 부터 모델과 뷰를 받은 뒤에 진행하는 작업은, 뷰 오브젝트에게 모델을 전달해주고 클라이언트에게 돌려줄 최종 결과물을 생성해달라고  
   요청하는 것이다. 최종 결과물은 HttpServletResponse 오브젝트 안에 담긴다.
   
7. HTTP 응답 돌려주기  
뷰 생성까지의 모든 작업을 마쳤으면 DispatcherServlet은 등록된 후처리기가 있는지 확인하고, 있다면 후처리기에서 후속 작업을 진행한 뒤에 뷰가 만드어준  
   HttpSevletResponse에 담긴 최종 결과를 서블릿 컨테이너에게 돌려준다.
   
   
#### DispatcherServlet의 DI 가능한 전략 
다양한 방식으로 DispatcherServlet의 동작방식과 기능을 확장, 변경할 수 있도록 준비된 전략들이 존재한다.
`HandlerMapping`  
핸들러 매핑은 URL과 요청 정보를 기준으로 어떤 핸들러 오브젝트, 즉 컨트롤러를 사용할 것인지를 결정하는 로직을 담당한다. DispatcherServlet은 하나 이상의  
핸들러 매핑을 가질 수 있다.  
`HandlerAdapter`  
핸들러 어댑터는 핸들러 매핑으로 선택한 컨트롤러/핸들러를 DispatcherServlet이 호출할 때 사용하는 어댑터다.  
@RequestMapping과 @Controller 애노테이션을 통해 정의되는 컨트롤러의 경우는 DefaultAnnotationHandlerMapping에 의해 핸들러가 결정되고, 그에  
대응되는 AnnotationMethodHandlerAdapter에 의해 호출이 일어난다.
`HandlerExceptionResolver`  
예외가 발생했을 때 이를 처리하는 로직을 갖고 있다.  
`ViewResolver`  
뷰 리졸버는 컨트롤러가 리턴한 뷰 이름을 찾고해서 적절한 뷰 오브젝트를 찾아주는 로직을 가진 전략 오브젝트다.


## 3.2 스프링 웹 애플리케이션 환경 구성
웹어플리케이션 컨텍스트를 구성하는 방법은 크게 세 가지가 있다. 그중 가장 보편적으로 사용되는 루트 컨텍스트와 서블릭 컨텍스트 두 개의 웹 애플리케이션 컨텍스트를  
사용하는 방법을 적용해보자. 서블릿 컨텍스트가 루트 컨텍스트를 부모 컨텍스트로 가지고, 자식 컨텍스트는 부모 컨텍스트를 참조할 수 있지만 그 반대는 안된다.  


## 3.3 컨트롤러 
서블릿이 넘겨주는 HTTP 요청은 HttpServletRequest 오브젝트에 담겨있다. 컨트롤러가 이를 이용해 사용자의 요청을 파악하려면 클라이언트 호스트, 포트, URI,  
쿼리스트링, 폼 파라미터, 쿠키, 헤더, 세션을 비롯해서 서블릿 컨테이너가 요청 애트리뷰트로 전달해주는 것까지 매우 다양한 정보를 참고해야 한다.  
컨트롤러가 요청을 받아주고 응답을 주는 등 수 많은 일을 해야되기 때문에 스프링 MVC가 컨트롤러 모델을 미리 제한하지 않고 어댑터 패턴을 사용해서라도 컨트롤러의 종류를  
필요에 따라 확장할 수 있도록 만든 이유가 바로 이 때문이다. DispatcherServlet의 전략 패턴을 통한 유연함의 가치가 가장 잘 드러나는 영역이 바로 컨트롤러다.  
이 절에서는 스프링 MVC가 제공하는 컨트롤러의 종류와 그에 따른 핸들러 어댑터를 알아보고, 필요에 따라 컨트롤러를 설계하고 만드는 법을 알아본다. 그리고 URL과  
핸들러를 연결해주는 핸들러 매핑에 대해서도 자세히 설명한다. 

### 3.3.1 컨트롤러의 종류와 핸들러 어댑터 
스프링 MVC가 지원하는 컨트롤러의 종류는 네 가지다. 각 컨트롤러를 DispatcherServlet에 연결해주는 핸들러 어댑터가 하니씩 있어야 하므로, 핸들러 어댑터도 네 개다.  
이 중에서 SimpleServletHandlerAdapter를 제외한 세 개의 핸들러 어댑터는 DispatcherServlet의 디폴트 전략으로 설정되어 있다.

#### Servlet과 SimpleServletHandlerAdapter 
첫 번째 컨트롤러 타입은 표준 서블릿이다. 표준 서블릿 인터페이스인 javax.servlet.Servlet을 구현한 서블릿 클래스를 스프링 MVC의 컨트롤러로 사용할 수 있다.  
서블릿을 web.xml에 등록하지 않고 컨트롤러에 등록했을 때의 장점은 서블릿 클래스 코드를 그대로 유지하면서 스프링 빈으로 등록된다는 점이다.  
서블릿을 스프링 MVC 컨트롤러로 사용하는 간단한 테스트 코드를 만들어보자.  
```java
public class ServletControllerTest extends AbstractDispatcherServletTest {
    @Test
   public void helloServletController() throws ServletException, IOException {
        setClass(SimpleServletHandlerAdapter.class, HelloServlet.class); // 핸들러 어댑터와 컨트롤러를 빈으로 등록
       initRequest("/hello").addParameter("name", "Spring");
    }

   @Component("/hello")
   static class HelloServlet extends HttpServlet {
      protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
         String name = req.getParameter("name");
         resp.getWriter().print("Hello" + name);
      }
   }
}
```

먼저 할 일은 서블릿 타입의 컨트롤러를 DispatcherServlet이 호출해줄 때 필요한 핸들러 어댑터를 등록하는 것이다. 
핸들러 어댑터가 빈으로 등록되어 있으면 DispatcherServlet은 이를 자동으로 감지해 디폴트 핸들러 어댑터를 대신해서 사용한다.  
AbstractDispatcherServletTest는 XML 설정파일 없이도 빈 클래스를 직접 제공해주는 방식으로 서블릿 컨텍스트에 빈을 등록할 수 있다. 그래서 이번 테스트에서는  
핸들러 어댑터를 setClasses() 메소드에 전달해서 빈으로 바로 등록해줬다.  
서블릿으로 만들어진 컨트롤러 빈도 등록해준다. 여기서 @Component는 빈 스캐닝 전략을 위해서가 아니라 단지 이름을 붙여주려고 사용했다.  
Servlet 타입의 컨트롤러는 모델과 뷰를 반환하지 않는다. 스프링 MVC의 모델과 뷰라는 개념을 알지 못하는 표준 서블릿을 그대로 사용한 것이기 때문이다. 그래서 결과는  
서블릿에서 HttpServletResponse에 넣어준 정보를 확인하는 방법을 사용한다. 

#### HttpRequestHandler와 HttpRequestHandlerAdapter
HttpRequestHandler는 인터페이스로 정의된 컨트롤러 타입이다. 이 인터페이스를 구현해서 컨트롤러를 만든다.  
```java
public interface HttpRequestHandler {
    void handlerRequest(HttpSevletRequest request, HttpServletResponse response) throws SevletException, IOException;
}
```

서블릿 인터페이스와 비슷하다. 실제로 서블릿처럼 동작하는 컨트롤러를 만들기 위해 사용한다. 전형적인 서블릿 스펙을 준수할 필요 없이 HTTP 프로토콜을 기반으로 한 전용  
서비스를 만들려고 할 때 사용할 수 있다.  
HttpRequestHandler는 모델과 뷰 개념이 없는 Http 기반의 RMI 같은 로우레벨 서비스를 개발할 때 이용할 수 있다는 사실 정도만 기억하고 넘어가자.  
HttpRequestHandlerAdapter는 디폴트 전략이므로 빈으로 등록해줄 필요는 없다.  

#### Controller와 SimpleControllerHandlerAdapter
```java
public interface Controller {
    ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception;
}
```

Controller 컨트롤러는 DispatcherServlet이 컨트롤러와 주고받는 정보를 그대로 메소드의 파라미터와 리턴 값으로 갖고 있다. 따라서 스프링 MVC의 가장 대표적인  
컨트롤러 타입이라고 볼 수 있다. 스프링 3.0의 애노테이션과 관례를 이용한 컨트롤러가 본격적으로 등장하기 전까지, 스프링 MVC 컨트롤러라고 하면 바로 이 Controller를  
들 수 있을 만큼 많이 사용되는 컨트롤러였다.  
Controller 타입의 컨트롤러는 인터페이스를 구현하기만 하면 되기 때문에 특정 클래스를 상속하도록 강제하는 여타 MVC 프레임워크의 컨트롤러보다 유연하게 컨트롤러  
클래스를 설계할 수 있다는 장점이 있다. 하지만 웹브라우저를 클라이언트로 갖는 컨트롤러로서의 필수 기능이 구현되어 있는 AbstractController를 상속해서 컨트롤러를  
만드는게 편리하기 때문에 이 방법이 권장 된다. AbstractController는 다음과 같은 웹 개발에 유용하게 쓸 수 있는 프로퍼티를 제공해준다. 
- synchronizeOnSession  
사용자가 자신의 HTTP 세션에 동시에 접근하는 것을 막아준다.
  
- supportedMethods  
컨트롤러가 허용하는 HTTP 메소드를 지정할 수 있다. 
  
- useExpiredHeader, userCacheControlHeader, useCacheControlNoStore, cacheSeconds  
이 네가지 프로퍼티는 HTTP 1.0/1.1의 Expires, Cahce-Control HTTP 헤더를 이용해서 브라우저의 캐시 설정정보를 보내줄 것인지를 결정한다.
  
컨트롤러의 구현 코드에서 비슷한 코드가 반복적으로 등장한다면, 이를 그대로 두지말고 공통적인 부분을 뽑아내서 기반 컨트롤러를 만들어야 한다. 

#### AnnotationMethodHandlerAdapter
다른 핸들러 어댑터와는 다르게 지원하는 컨트롤러의 타입 정해져 있지 않다. 다른 핸들러 어댑터는 특정 인터페이스를 구현한 컨트롤러만 지원한다.  
이 어댑터는 컨트롤러 타입에는 제한이 없지만 클래스와 메소드에 붙은 몇가지 애노테이션의 정보와 메소드 이름, 파라미터, 리턴 타입에 대한 규칙 등을 종합적으로 분석해서  
컨트롤러를 선별하고 호출 방식을 결정한다.  
또 다른 특징은 컨트롤러 하나가 하나 이상의 URL에 매핑될 수 있다는 점이다. 여타 컨트롤러는 특정 인터페이스를 구현하면 그 인터페이스의 대표 메소드를 통해 컨트롤러가  
호출되기 때문에, 특별한 확장 기능을 사용하지 않으면 URL당 하나의 컨트롤러가 매핑된다. 이러면 요청 개수에 따라 컨트롤러의 숫자도 급격하게 늘어난다.  
AnnotationMethodHandlerAdapter는 DefaultAnnotationHandlerMapping 핸들러 매핑과 함께 사용해야 한다. 두 가지 모두 동일한 어노테이션을 사용하기  
때문이다.

```java
@Controller
public class HelloController {
    @RequestMapping("/hello")
   public String hello(@RequestParam("name") String name, ModelMap map) {
        map.put("message", "Hello " + name);
        return "/WEB_INF/view/hello.jsp"
    }s
}
```

### 3.3.2 핸들러 매핑
핸들러 매핑은 HTTP 요청정보를 이용해서 이를 처리할 핸들러 오브젝트, 즉 컨트롤러를 찾아주는 기능을 가진 DispatcherServlet의 전략이다. 하나의 핸들러 매핑  
전략이 여러 가지 타입의 컨트롤러를 선택할 수 있다. 스프링은 기본적으로 다섯 가지 핸들러 매핑을 제공한다.  
이 중에서 디폴트로 등록된 핸들러 매핑은 BeanNameUrlHandlerMapping과 DefaultAnnotationHandlerMappping이다. 그 외의 핸들러 매핑을 사용하려면  
핸들러 매핑 클래스를 빈으로 등록해줘야 한다. 

#### BeanNameUrlHandlerMapping  
빈의 이름에 들어 있는 URL을 HTTP 요청의 URL과 비교해서 일치하는 빈을 찾아준다.  
예를 들어 다음 빈 선언은 /s로 시작하는 /s, /sl, /sabcd 같은 URL에 매핑된다.
```xml
<bean name="/s*" class="springbook...Contoller"></bean>
```

#### ControllerBeanNameHandlerMapping
이 핸들러 매핑은 빈의 아이디나 빈 이름을 이용해 매핑해주는 핸들러 매핑 전략이다.

#### ControllerClassNameHandlerMapping
빈 이름 대신 클래스 이름을 URL에 매핑해주는 핸들러 매핑 클래스다.

#### SimpleUrlHandlerMapping
URL과 컨트롤러의 매핑정보를 한 곳에 모아놓을 수 있는 핸들러 매핑 전략이다.

#### DefaultAnnotationHandlerMapping
@RequestMapping이라는 애노테이션을 컨트롤러 클래스나 메소드에 직접 부여하고 이를 이용해 매핑하는 전략이다. 메소드 단위로 URL을 매핑해줄 수 있어서 컨트롤러의  
개수를 획기적으로 줄일 수 있다는 장점이 있다. 또한 URL 뿐 아니라 GET/POST와 같은 HTTP 메소드, 파라미터와 HTTP 헤더정보까지 매핑에 활용할 수 있다. 

#### 기타 공통 설정정보
order  
핸들러 매핑은 한 개 이상을 동시에 사용할 수 있다. 중복되는 경우 우선 순위를 설정할 수 있다.

defaultHandler  
URL을 매핑할 대상을 찾지 못했을 경우 자동으로 디폴트 핸들러를 선택해준다.

alwaysUseFullPath  


### 3.3.3 핸들러 인터셉터
핸들러 매핑의 역할은 기본적으로 URL과 요청정보로부터 컨트롤러 빈을 찾아주는 것이다. 그런데 한가지 중요한 기능이 더 있다. 핸들러 인터셉터를 적용해주는 것이다.  
핸들러 인터셉터는 DispatcherServlet이 컨트롤러를 호출하기 전과 후에 요청과 응답을 참조하거나 가공할 수 있는 일종의 필터다. 서블릿 필터와 유사한 개념이라고 보면  
된다.  
핸들러 매핑의 역할은 URL로부터 컨트롤러만 찾아주는 것이 아니다. 핸들러 매핑은 DispatcherServlet으로부터 매핑 작업을 요청받으면 그 결과로 핸들러 실행 체인  
(HandlerExecutionChain)을 돌려준다. 이 핸들러 실행 체인은 하나 이상의 핸들러 인터셉터를 거쳐서 컨트롤러가 실행될 수 있도록 구성되어 있다.  
핸들러 인터셉터는 서블릿 필터와 그 쓰임새가 유사하지만 핸들러 인터셉터는 HttpServletRequest, HttpServletResponse 뿐 아니라, 실행될 컨트롤러 빈 오브젝트,  
컨트롤러가 돌려주는 ModelAndView, 발생한 예외 등을 제공받을 수 있기 때문에 서블릿 필터보다 더 정교하고 편리하게 인터셉터를 만들 수 있다.

#### HandlerInterceptor
핸들러 인터셉터는 HandlerInterceptor 인터페이스를 구현해서 만든다. 이 인터페이스 안에는 세 개의 메소드가 포함되어 있다.

- boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object Handler) throws Exception  
이 메소드는 컨트롤러가 호출되기 전에 실행된다. 컨트롤러 실행 이전에 처리해야 할 작업이 있거나 로그를 남기기 위해 사용한다.  
  리턴 값이 true면 핸들러 실행 체인의 다음 단계로 진행되고 false면 작업을 중단한다.
  
- boolean postHandle(HttpServletRequest request, HttpServletResponse response, Object Handler,
  ModelAndView modelAndView) throws Exception  
  후처리 작업을 진행할 수 있다.

- boolean afterCompletion(HttpServletRequest request, HttpServletResponse response, Object Handler,
  Exception ex) throws Exception  
  모든 뷰에서 최종 결과를 생성하는 일을 포함한 모든 작업이 다 완료된 후에 실행된다.
  
핸들러 인터셉터는 하나 이상을 등록할 수 있다. preHandler()은 인터셉터가 등록된 순으로 실행되고 postHandler()는 반대로 실행된다.

#### 핸들러 인터셉터 적용
핸들러 인터셉터를 사용하려면 먼저 핸들러 매핑 클래스를 빈으로 등록해야 한다. 핸들러 매핑 빈의 interceptors 프로퍼티를 이용해 핸들러 인터셉터 빈의 레퍼런스를  
넣어주면 된다.  
핸들러 인터셉터는 서블릿 필터와 기능이나 용도가 비슷하다. 그래서 둘 중 어떤 것을 사용할지 신중하게 선택해야 한다.  
서블릿 필터는 web.xml에 별도로 등록해줘야 하고 필터 자체는 스프링의 빈이 아니다. 반면에 모든 요청에 적용된다는 장점이 있다.  
핸들러 인터셉터는 적용 대상이 DispatcherServlet의 특정 핸들러 매핑으로 제한된다는 제약이 있지만 스프링의 빈으로 등록할 수 있고, 컨트롤러 오브젝트와  
ModelAndView와 같은 정보를 사용할 수 있다는 장점이 있다.
  

