# 4장 스프링 @MVC
스프링은 DispatcherServlet과 7가지 전략을 기반으로 한 MVC 프레임워크를 제공한다. 스프링 MVC 프레임워크의 장점은 유연한 확장이 가능하도록 설계된 MVC 엔진인  
DispatcherServlet이다. 스프링 3.0부터 한층 더 강화된 어노테이션 전략을 활용해서 가장 혁신적인 발전이 생겼다.  
애노테이션을 중심으로 한 새로운 MVC의 확장 기능은 @MVC라는 별칭으로도 불린다. @MVC는 스프링 3.0에서 기존에 가장 많이 사용되던 Controller 타입의 기반  
클래스들을 대부분 대체하게 되었다. 

## 4.1 @RequestMapping 핸들러 매핑
@MVC의 가장 큰 특징은 핸들러 매핑과 핸들러 어댑터의 대상이 오브젝트가 아니라 메소드라는 점이다. @MVC에서는 모든 것이 메소드 레벨로 세분화됐다. 애노테이션은  
부여되는 대상의 타입이나 코드에는 영향을 주지 않는 메타정보이므로 훨씬 유연한 방식으로 컨트롤러를 구성할 수 있게 해준다.  
@MVC의 핸들러 매핑을 위해서는 DefaultAnnotationHandlerMapping이 필요하다. 디폴트 핸들러 매핑 전략이므로 다른 핸들러 매핑 빈을 명시적으로 등록하지  
않았다면 기본적으로 사용할 수 있다. 

### 4.1.1 클래스/메소드 결합 매핑정보
DefaultAnnotationHandlerMapping의 핵심은 매핑정보로 @RequestMapping 애노테이션을 활용한다는 점이다. 스프링은 클래스 레벨과 메소드 레벨에 붙은  
@RequestMapping의 정보를 결합해서 최종 매핑정보를 생성한다. 기본적인 결합 방법은 타입 레벨의 @RequestMapping 정보를 기준으로 삼고, 메소드 레벨의  
@RequestMapping정보는 타입 레벨의 매핑을 더 세분화하는 데 사용하는 것이다. 

#### @RequestMapping 애노테이션
- String[] value(): URL 패턴  
URL 패턴을 지정한다. "/user/{userId}" 처럼 {}을 사용하는 URI 템플릿을 사용할 수도 있다. {}에 들어가는 이름은 패스 변수라고 불리며, 하나 이상 등록할 수  
  있다. URL 패턴은 배열이기 때문에 하나 이상의 URL을 등록할 수 있다. URL 패턴에서 기억해야할 중요한 사실은 디폴트 접미어 패턴이 적용된다는 점이다.   
  @RequestMapping("/hello") 처럼 확장자가 붙지 않고 /로 끝나지도 않는 URL 패턴에는 디폴트 접미어 패턴이 적용되어서 다음 세개의 URL 패턴을 적용했을 때와  
  동일한 결과가 나온다. @RequestMapping({"/hello", "/hello/", "/hello.*"})
  

- RequestMethod[] method(): HTTP 요청 메소드  
GET, HEAD, POST, PUT, DELETE, OPTIONS, TRACE 7개의 HTTP 메소드가 정의되어 있다. 같은 URL이라고 하더라도 메소드에 따라 구분할 수 있다.  
  때로는 타입 레벨에는 URL만 주고 메소드 레벨에는 HTTP 요청 메소드만 지정하는 방법을 사용하기도 한다. 이럴때는 URL을 생략할 수도 있다.
  

- String[] params(): 요청 파라미터  
세 번째 매핑 방식은 요청 파라미터와 그 값을 비교해서 매핑해주는 것이다. 같은 URL을 사용하더라도 HTTP 요청 파라미터에 따라 별도의 작업을 해주고 싶을 때가 있다.  
  이때는 코드에서 파라미터를 검사해서 기능을 분리하는 대신 @ReqeustMapping에 매핑을 위한 요청 파라미터를 지정해줄 수 있다. 파라미터는 '타입=값' 형식으로  
  지정해주면 된다. 같은 URL이더라도 파라미터에 따라 다르게 매핑이 된다. 조건을 만족하는 매핑이 여러개 있는 경우는 자세하게 매핑되어 있는 쪽을 우선 시 한다.  
  특정 파라미터가 존재하지 않아야 매핑이 되게 하고 싶으면 params="!type" 처럼 !를 앞에 붙여주면 된다.
  
- String[] headers(): HTTP 헤더  
params와 비슷하게 '헤더이름 = 값'의 형식으로 사용한다.
  
#### 타입 레벨 매핑과 메소드 레벨 매핑의 결합
타입 레벨에 붙는 @RequestMapping은 타입 내의 모든 매핑용 메소드의 공통 조건을 지정할 때 사용한다. 그리고 메소드 레벨에서 조건을 세분화해주면 된다.  
터압 래밸의 URL 패턴에 *나 **을 사용했을 때도 URL을 결합할 수 있다. 타입 레벨에 /user 대신 /user/*를 사용했을 경우 메소드 레벨에 /add가 선언되어 있으면  
/user/add로 결합된다. 타입 레벨에 /user/**로 되어 있다면 메소드 레벨의 /add는 /user/**/add로 결합된다. 

#### 메소드 레벨 단독 매핑
메소드 레벨의 매핑조건에 공통점이 없는 경우라면 타입 레벨에서는 조건을 주지 않고 메소드 레벨에서 독립적으로 매핑정보를 지정할 수 있다. 이때 타입 레벨에는 조건이 없는  
@RequestMapping을 붙여두면 된다. 컨트롤러 클래스에 @Controller 애노테이션을 붙여서 빈 자동스캔 방식으로 등록되게 했다면 클래스 레벨의 @RequestMapping을  
생략할 수도 있다.

#### 타입 레벨 단독 매핑
핸들러 매핑은 원래 핸들러 오브젝트를 결정하는 전략이다. 클래스 레벨의 URL 패턴이 /*로 끝나는 경우에는 메소드 레벨의 URL 패턴으로 메소드 이름이 사용되게 할 수 있다.


## 4.2 @Controller
DefaultAnnotationHandlerMapping은 사용자 요청을 @ReqeustMapping 정보를 활용해서 컨트롤러 빈의 메소드에 매핑해준다. 그리고  
AnnotationMethodHandlerAdapter는 매핑된 메소드를 실제로 호출하는 역할을 담당한다. 다른 핸들러 어댑터들은 컨트롤러가 구현하고 있는 인터페이스의 실행 메소드를  
알고 있기 때문에 이를 이용해 간단히 메소드를 호출할 수 있었다. 하지만 @MVC의 컨트롤러는 특정 인터페이스를 구현하지 않는다. 그렇다면 어떻게 이런 메소드를 컨트롤러  
메소드로 사용할 수 있는 것일까?  
 Controller은 DispatcherServlet과 핸들러 어댑터의 경우와 동일하게 HttpServletRequest, HttpServletResponse를 전달받고 ModelAndView 타입을  
리턴한다. 따라서 Controller 타입의 컨트롤러를 담당한느 핸들러 어댑터가 해주는 일은 거의 없다. 
3장에서 만든 SimpleController의 경우 HttpServlet을 숨기고 parameter를 전달해서 요청을 처리했다. 하지만 어떤 경우는 쿠키나 헤더 정보를 참고해야하기 때문에  
HttpServlet 관련 데이터가 필요하다.  
@Controller를 사용하면 유연성과 편의성을 살릴 수 있다.  
@Controller에서는 컨트롤러 역할을 담당하는 메소드의 파라미터 개수와 타입, 리턴 타입 등을 자유롭게 결정할 수 있다.  
@MVC와 @Controller 개발 방법은 스프링 역사상 가장 획기적인 변신이다. 기존 영역의 기술에도 지속적으로 새로운 기능을 추가해오기는 했지만, 이전에 사용하던 방식을  
버리고 새로운 방식을 사용하도록 강하게 권장한 적은 없었다. 스프링은 1.x 시절부터 Controller 타입의 편리한 기반 컨트롤러를 제공해왔다. 하지만 스프링 3.0에서  
이런 기반 컨트롤러는 AbstractController 정도를 제외하면 대부분 @Deprecated가 붙어 있다. 

### 4.2.1 메소드 파라미터 종류

#### HttpServletRequest, HttpServletResponse

#### HttpSession
HttpSession 오브젝트는 HttpServletRequest를 통해 가져올 수도 있지만, HTTP 세션만 필요한 경우라면 파라미터를 선언해 직점 받는 편이 낫다.

#### WebRequest, NativeWebRequest
HttpServletRequest의 요청정보를 대부분 그대로 갖고 있는 서블릿 API에 종속적이지 않은 오브젝트 타입이다.

#### Locale

#### InputStream, Reader
HttpServletRequest의 getInputStream()을 통해서 받을 수 있는 콘텐트 스트림 또는 Reader 타입 오브젝트를 받을 수 있다.

#### OutputStream, Writer
HttpServletResponse의 getOutStream()을 통해서 받을 수 있는 콘텐트 스트림 또는 Writer 타입 오브젝트를 받을 수 있다.

#### @PathVariable
URL에 {}로 들어가는 패스 변수를 받는다.

#### @RequestParam
단일 HTTP 요청 파라미터를 메소드 파라미터에 넣어주는 애노테이션이다. @RequestParam에 파라미터 이름을 지정하지 않고 Map<String, String> 타입으로 선언하면  
모든 요청 파라미터를 담은 맵으로 받을 수 있다.  
파라미터를 필수가 아니라 선택적으로 제공하게 하려면 required=false로 지정해주면 되고 defaultValue=1 로 기본 값을 지정할 수도 있다.

#### @CookieValue

#### @RequestHeader
요청 해더 정보를 메소드 파라미터에 넣어준다.

#### Map, Model, ModelMap
모델정보를 담는 데 사용할 수 있는 오브젝트가 전달된다.

#### @ModelAttribute
컨트롤러가 사용하는 모델 중에는 클라이언트로부터 받는 HTTP 요청정보를 이용해 생성되는 것이 있다. 이렇게 클라이언트로부터 컨트롤러가 받는 요청정보 중에서 하나 이상의  
값을 가진 오브젝트 형태로 만들 수 있는 구조적인 정보를 @ModelAttribute 모델이라고 부른다.  
도메인 오브젝트나 DTO의 프로퍼티에 요청 파라미터를 바인딩해서 한 번에 받으면 @ModelAttribute라고 볼 수 있다.  
추가적인 기능은 컨트롤러가 리턴하는 모델에 파라미터로 전달한 오브젝트를 자동으로 추가해준다.

#### Error, BindingResult
@ModelAttribute가 붙은 파라미터를 처리할 때는 @RequestParam과 달리 검증 작업이 추가적으로 진행된다. @ModelAttribute는 타입 변환 문제를 바로 처리하지  
않는다. @ModelAttribute 입장에서는 파라미터 타입이 일치하지 않는다는 건 검증 작업의 한 가지 결과일 뿐이지, 예상치 못한 예외 상황이 아니라는 뜻이다.  
이 때문에 @ModelAttribute를 통해 폼의 정보를 전달받을 때는 Errors 또는 BindingResult 타입의 파라미터를 같이 사용해야 한다. 이 두가지 오브젝트에는  
파라미터를 바인딩하다가 발생한 변환 오류와 모델 검증기를 통해 검증하는 중에 발견한 오류가 저장된다. 이 두 가지 타입의 파라미터는 반드시 @ModelAttribute  
파라미터 뒤에 나와야 한다.

#### SessionStatus
모델 오브젝트를 세션에 저장했다가 다음 페이지에서 다시 활용하게 해주는 기능이 있는데 이 기능을 사용하다가 더 이상 세션 내에 모델 오브젝트를 저장할 필요가 없을 경우에  
코드에서 직접 작업 완료 메소드를 호출해서 세션 안에 저장된 오브젝트를 제거해줘야한다.

#### @RequestBody
HTTP 요청의 본문 부분이 그대로 전달된다. AnnotationMethodHandlerAdapter에는 HttpMessageConverter 타입의 메시지 변환기가 여러 개 등록되어 있다.  
@RequestBody가 붙은 파라미터가 있으면 HTTP 요청의 미디어 타입과 파라미터 타입을 먼저 확인한다. 메시지 변환기 중에서 해당 미디어 타입과 파라미터 타입을 처리할  
수 있는 것이 있다면, HTTP 요청의 본문 부분을 통째로 변환해서 지정된 메소드 파라미터로 전달해준다. Json 메시지라면 MappingJacksonHttpMessageConverter를  
사용할 수 있다. @RequestBody는 보통 @ResponseBody와 함께 사용된다.

#### @Value
빈의 값 주입에서 사용하던 @Value 애노테이션도 컨트롤러 메소드 파라미터에 부여할 수 있다. 주로 시스템 프로퍼티나 다른 빈의 프로퍼티 값, 또는 좀 더 복잡한 SpEL을  
이용해 클래스의 상수를 읽어오거나 특정 메소드를 호출한 결과 값, 조건식 등을 넣을 수 있다.

#### @Valid
JSR-303의 빈 검증기를 이용해서 모델 오브젝트를 검증하도록 지시하는 지시자이다. 보통 @ModelAttribute와 함께 사용한다.

### 4.2.2 리턴 타입의 종류
컨트롤러가 DispatcherServlet에게 돌려줘야하는 정보는 모델과 뷰다. 드물지만 ModelAndView는 무시하고 HttpServletResponse에 직접 결과를 넣어 리턴하는  
경우도 있다. 

#### 자동 추가 모델 오브젝트와 자동생성 뷰 이름
- @ModelAttribute 모델 오브젝트 또는 커맨드 오브젝트  
메소드 파라미터 중에서 @ModelAttribute를 붙인 모델 오브젝트나 단순 타입이 아니라서 커맨드 오브젝트로 처리되는 오브젝트라면 자동으로 컨트롤러가 리턴하는 모델에  
  추가된다. 
  
- Map, Model ModelMap 파라미터  
컨트롤러 메소드에 Map, MOdel, ModelMap 타입의 파라미터를 사용하면 미리 생성된 모델 맵 오브젝트를 전달받아서 오브젝트를 추가할 수 있다.  
  
- @ModelAttribute 메소드  
@ModelAttribute는 컨트롤러 클래스의 일반 메소드에도 부여할 수 있다. 뷰에서 참고정보로 사용되는 모델 오브젝트를 생성하는 메소드를 지정하기 위해 사용된다.
@ModelAttribute가 붙은 메소드는 컨트롤러 클래스 안에 정의하지만 컨트롤러 기능을 담당하지 않는다. 클래스 내의 모든 컨트롤러의 모델에 자동 추가된다.
  
- BindingResult  
@ModelAttribute 파라미터와 함께 사용하는 BindingResult 타입의 오브젝트도 모델에 자동으로 추가된다. 
  
컨트롤러에서 어떤 식으로든 뷰 정보를 제공해주지 않는 경우에는 RequestToViewNameTranslator 전략에 의해 자동으로 뷰 이름이 만들어진다. 

#### ModelAndView
@Controller에서는 ModelAndView를 이용하는 것보다 편리한 방법이 많아서 자주 사용되지는 않는다. 

#### String 
메소드의 리턴 타입이 스트링이면 이 리턴 값은 뷰 이름으로 사용된다. 모델정보는 모델 맵 파라미터로 가져와 추가해주는 방법을 사용해야 한다.

#### void
return 타입이 void인 경우 RequestToViewNameResolver 전략을 통해 자동생성되는 뷰 이름이 사용된다.

#### 모델 오브젝트 
뷰 이름은 RequestToViewNameResolver로 자동샌성하는 것을 사용하고 코드를 이용해 모델에 추가할 오브젝트가 하나뿐이라면 Model 파라미터를 받아서 저장하는 대신  
모델 오브젝트를 바로 리턴해도 된다.

#### Map/Model/ModelMap
메소드의 코드에서 Map이나 Model, ModelMap 타입의 오브젝트를 직접 만들어서 리턴해주면 이 오브젝트는 모델로 사용된다.

#### View
String으로 view 이름을 return하는 것 대신에 직접 뷰 객체를 리턴할 수도 있다.

#### @ResponseBody
@ResponseBody가 메소드 레벨에 부여되면 메소드가 리턴하는 오브젝트는 뷰를 통해 결과를 만들어내는 모델로 사용되는 대신, 메시지 컨버터를 통해 바로 HTTP 응답의  
메시지 본문으로 전환된다.

### 4.2.3 @SessionAttribute와 SessionStatus
HTTP 요청에 의해 동작하는 서블릿은 기본적으로 상태를 유지하지 않지만 애플리케이션은 기본적으로 상태를 유지할 필요가 있다. 

#### @SessionAttribute
클래스 레벨에 @SessionAttribute("user") 와 같은 어노테이션을 달아줄 수 있다.  
@SessionAttribute가 해주는 기능은 두가지이다. 첫째, 컨트롤러 메소드가 생성되는 모델정보 중에서 @SessionAttributes에 지정한 이름과 동일한 것이 있다면  
이를 세션에 저장해준다. 두번째는 @ModelAttribute가 지정된 파라미터가 있을 때 이 파라미터에 전달해줄 오브젝트를 세션에서 가져오는 것이다. 원래 파라미터에  
@ModelAttribute가 있으면 해당 타입의 새 오브젝트를 생성한 후에 요청 파라미터 값을 프로퍼티에 바인딩해준다. 그런데 @SessionAttributes에 선언된 이름과  
@ModelAttribute의 모델 이름이 동일하다면 그때는 먼저 세션에 같은 이름의 오브젝트가 존재하는지 확인한다. 만약 존재한다면 모델 오브젝트를 새로 만드는 대신 세션에  
있는 오브젝트를 가져와 @ModelAttribute 파라미터로 전달해줄 오브젝트로 사용한다.

#### SessionStatus
@SessionAttribute를 사용할 때는 더 이상 필요없는 세션 애트리뷰트를 코드로 제거해줘야 한다는 점을 잊지 말자. @SessionAttributes를 사용할 때는  
SessionStatus를 이용해 세션을 정리해주는 코드가 항상 같이 따라다녀야 한다는 사실을 기억해두자.

