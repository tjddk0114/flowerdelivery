# flowerdelivery
intensive lv2 course  group 3

# 꽃 배달 서비스 ( 3조 리포트 - 이기정 개인 평가  )
![image](https://user-images.githubusercontent.com/80744199/117618717-52b33e00-b1a9-11eb-917b-6dafcedd86e8.png)

# Table of contents

- [조별과제 - 꽃 배달 서비스](#---)
  - [서비스 시나리오](#서비스-시나리오)
  - [체크포인트](#체크포인트)
  - [분석/설계](#분석설계)
  - [구현:](#구현)
    - [DDD 의 적용](#ddd-의-적용)    
    - [동기식 호출 과 Fallback 처리](#동기식-호출-과-Fallback-처리)
    - [비동기식호출](#비동기식호출)
    - [폴리글랏 퍼시스턴스](#폴리글랏-퍼시스턴스)
    - [폴리글랏 프로그래밍](#폴리글랏-프로그래밍)
    - [API게이트웨이](#API게이트웨이)
    - [SAGA패턴 적용](#SAGA패턴적용)
  - [운영](#운영)
    - [CI/CD 설정](#cicd설정)
    - [동기식 호출 / 서킷 브레이킹 / 장애격리](#동기식-호출-서킷-브레이킹-장애격리)
    - [오토스케일 아웃](#오토스케일-아웃)
    - [무정지 재배포](#무정지배포)
    - [Configmap](#ConfigMap)
    - [livenessProbe](#livenessProbe)
  
# 서비스 시나리오

기능적 요구사항
1. 고객(Customer)이 메뉴를 선택하여 주문한다
2. 고객이 결제한다
3. 주문이 되면 주문 내역이 꽃상점주인(Store)에게 전달된다
4. 상점주인이 주문 내역을 확인하여 꽃을 데코레이션 한다
5. 꽃 데코레이션이 완료되면 배달대행서비스에 배달 요청 내역이 전달된다
6. 주문한 꽃이나 자재가 부족할 경우 꽃상점에서 주문을 거절할 수 있다
7. 주문이 거절될 경우 결제가 취소(강제취소) 된다
8. 배달기사(Rider)는 배송상태 메뉴에서 배달 요청된 꽃 주문 내역을 확인할 수 있다
9. 배달기사가 꽃 배송을 시작한다
10. 배달기사가 꽃 배송을 완료한다
11. 고객이 주문을 취소할 수 있다
12. 주문이 취소되면 배달이 취소된다
13. 고객이 주문상태를 중간중간 조회한다
14. 주문/배송상태가 바뀔 때 마다 MyPage에서 상태를 확인할 수 있다.

비기능적 요구사항
1. 트랜잭션
    1. 결제가 되지 않은 주문건은 아예 거래가 성립되지 않아야 한다  Sync 호출 
2. 장애격리
    1. 상점관리 기능이 수행되지 않더라도 주문은 365일 24시간 받을 수 있어야 한다  Async (event-driven), Eventual Consistency
    2. 결제시스템이 과중되면 사용자를 잠시동안 받지 않고 결제를 잠시후에 하도록 유도한다  Circuit breaker, fallback
3. 성능
    1. 고객이 자주 상점관리에서 확인할 수 있는 배달상태를 주문시스템(프론트엔드)에서 확인할 수 있어야 한다  CQRS
    


# 체크포인트

- 분석 설계


  - 이벤트스토밍: 
    - 스티커 색상별 객체의 의미를 제대로 이해하여 헥사고날 아키텍처와의 연계 설계에 적절히 반영하고 있는가?
    - 각 도메인 이벤트가 의미있는 수준으로 정의되었는가?
    - 어그리게잇: Command와 Event 들을 ACID 트랜잭션 단위의 Aggregate 로 제대로 묶었는가?
    - 기능적 요구사항과 비기능적 요구사항을 누락 없이 반영하였는가?    

  - 서브 도메인, 바운디드 컨텍스트 분리
    - 팀별 KPI 와 관심사, 상이한 배포주기 등에 따른  Sub-domain 이나 Bounded Context 를 적절히 분리하였고 그 분리 기준의 합리성이 충분히 설명되는가?
      - 적어도 3개 이상 서비스 분리
    - 폴리글랏 설계: 각 마이크로 서비스들의 구현 목표와 기능 특성에 따른 각자의 기술 Stack 과 저장소 구조를 다양하게 채택하여 설계하였는가?
    - 서비스 시나리오 중 ACID 트랜잭션이 크리티컬한 Use 케이스에 대하여 무리하게 서비스가 과다하게 조밀히 분리되지 않았는가?
  - 컨텍스트 매핑 / 이벤트 드리븐 아키텍처 
    - 업무 중요성과  도메인간 서열을 구분할 수 있는가? (Core, Supporting, General Domain)
    - Request-Response 방식과 이벤트 드리븐 방식을 구분하여 설계할 수 있는가?
    - 장애격리: 서포팅 서비스를 제거 하여도 기존 서비스에 영향이 없도록 설계하였는가?
    - 신규 서비스를 추가 하였을때 기존 서비스의 데이터베이스에 영향이 없도록 설계(열려있는 아키택처)할 수 있는가?
    - 이벤트와 폴리시를 연결하기 위한 Correlation-key 연결을 제대로 설계하였는가? 

  - 헥사고날 아키텍처
    - 설계 결과에 따른 헥사고날 아키텍처 다이어그램을 제대로 그렸는가?
    
- 구현
  - [DDD] 분석단계에서의 스티커별 색상과 헥사고날 아키텍처에 따라 구현체가 매핑되게 개발되었는가?
    - Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 데이터 접근 어댑터를 개발하였는가
    - [헥사고날 아키텍처] REST Inbound adaptor 이외에 gRPC 등의 Inbound Adaptor 를 추가함에 있어서 도메인 모델의 손상을 주지 않고 새로운 프로토콜에 기존 구현체를 적응시킬 수 있는가?
    - 분석단계에서의 유비쿼터스 랭귀지 (업무현장에서 쓰는 용어) 를 사용하여 소스코드가 서술되었는가?
  - Request-Response 방식의 서비스 중심 아키텍처 구현
    - 마이크로 서비스간 Request-Response 호출에 있어 대상 서비스를 어떠한 방식으로 찾아서 호출 하였는가? (Service Discovery, REST, FeignClient)
    - 서킷브레이커를 통하여  장애를 격리시킬 수 있는가?
  - 이벤트 드리븐 아키텍처의 구현
    - 카프카를 이용하여 PubSub 으로 하나 이상의 서비스가 연동되었는가?
    - Correlation-key:  각 이벤트 건 (메시지)가 어떠한 폴리시를 처리할때 어떤 건에 연결된 처리건인지를 구별하기 위한 Correlation-key 연결을 제대로 구현 하였는가?
    - Message Consumer 마이크로서비스가 장애상황에서 수신받지 못했던 기존 이벤트들을 다시 수신받아 처리하는가?
    - Scaling-out: Message Consumer 마이크로서비스의 Replica 를 추가했을때 중복없이 이벤트를 수신할 수 있는가
    - CQRS: Materialized View 를 구현하여, 타 마이크로서비스의 데이터 원본에 접근없이(Composite 서비스나 조인SQL 등 없이) 도 내 서비스의 화면 구성과 잦은 조회가 가능한가?

  - 폴리글랏 플로그래밍
    - 각 마이크로 서비스들이 하나이상의 각자의 기술 Stack 으로 구성되었는가?
    - 각 마이크로 서비스들이 각자의 저장소 구조를 자율적으로 채택하고 각자의 저장소 유형 (RDB, NoSQL, File System 등)을 선택하여 구현하였는가?
  - API 게이트웨이
    - API GW를 통하여 마이크로 서비스들의 집입점을 통일할 수 있는가?
    - 게이트웨이와 인증서버(OAuth), JWT 토큰 인증을 통하여 마이크로서비스들을 보호할 수 있는가?
- 운영
  - SLA 준수
    - 셀프힐링: Liveness Probe 를 통하여 어떠한 서비스의 health 상태가 지속적으로 저하됨에 따라 어떠한 임계치에서 pod 가 재생되는 것을 증명할 수 있는가?
    - 서킷브레이커, 레이트리밋 등을 통한 장애격리와 성능효율을 높힐 수 있는가?
    - 오토스케일러 (HPA) 를 설정하여 확장적 운영이 가능한가?
    - 모니터링, 앨럿팅: 
  - 무정지 운영 CI/CD (10)
    - Readiness Probe 의 설정과 Rolling update을 통하여 신규 버전이 완전히 서비스를 받을 수 있는 상태일때 신규버전의 서비스로 전환됨을 siege 등으로 증명 
    - Contract Test :  자동화된 경계 테스트를 통하여 구현 오류나 API 계약위반를 미리 차단 가능한가?


# 분석/설계


## AS-IS 조직 (Horizontally-Aligned)
  ![image](https://user-images.githubusercontent.com/487999/79684144-2a893200-826a-11ea-9a01-79927d3a0107.png)

## TO-BE 조직 (Vertically-Aligned)
  ![image](https://user-images.githubusercontent.com/80744199/117759050-10980400-b25e-11eb-845d-2bffcaaef09e.png)

## Event Storming 결과
* MSAEz 로 모델링한 이벤트스토밍 결과: 
=> 꽃배달 서비스 http://www.msaez.io/#/storming/5nRSKjrx87XLm3BPJHqFNcoc9ZT2/mine/89e807d4cea32297228749710093e35c

3조 이기정 개인 모델링 링크 
http://www.msaez.io/#/storming/5nRSKjrx87XLm3BPJHqFNcoc9ZT2/mine/508af93c82fba739ecbecb2d68f9701d


## 전체
![image](https://user-images.githubusercontent.com/80744199/118610060-591f6680-b7f6-11eb-92d2-247747f6d08f.png)

신규 서비스 추가 된 버전 
![image](https://user-images.githubusercontent.com/80744199/121293773-27964880-c927-11eb-8aa9-31b33d5ad40d.png)

## 주문
![image](https://user-images.githubusercontent.com/80744199/118610157-6f2d2700-b7f6-11eb-9325-2cb570182359.png)

## 결제
![image](https://user-images.githubusercontent.com/80744199/118610178-748a7180-b7f6-11eb-8618-cc3bf12a07df.png)

## 주문관리
![image](https://user-images.githubusercontent.com/80744199/118610231-81a76080-b7f6-11eb-9067-8bf74b05e591.png)

## 배송
![image](https://user-images.githubusercontent.com/80744199/118610263-8b30c880-b7f6-11eb-8434-5a7bea500fd0.png)

## 상품
![image](https://user-images.githubusercontent.com/80744199/121293704-03d30280-c927-11eb-92b4-791fbdfd4b6f.png)



## 헥사고날 아키텍처 다이어그램 도출
꽃배달서비스 - 헥사고날 아키텍처

![image](https://user-images.githubusercontent.com/80744199/118609005-49ebe900-b7f5-11eb-98d0-3871a043d721.png)


상품(아이템)서비스 신규 추가 

![image](https://user-images.githubusercontent.com/80744199/121293370-7abbcb80-c926-11eb-9812-1baac649602c.png)



# 구현:

분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 바운더리 컨텍스트 별로 표현된 서비스를 스트링 부트로 구현함 

적용 아키텍쳐는 아래와 같으며

![image](https://user-images.githubusercontent.com/80744199/119110084-bae10a00-ba5c-11eb-8fb4-ec4ef68e4421.png)


상품관리를 위해 아이템 서비스 신규 추가

![image](https://user-images.githubusercontent.com/80744199/121293025-e6516900-c925-11eb-9ad3-3921ee9985e4.png)

각 서비스별 구동커맨드는 아래와 같음
```
cd gateway
mvn spring-boot:run 
포트 : 8088

cd order 
mvn spring-boot:run 
포트 : 8081 

cd payment
mvn spring-boot:run   
포트 : 8082

cd ordermanagement
mvn spring-boot:run  
포트 : 8083

cd delivery
mvn spring-boot:run
포트 : 8084 

cd item
mvn spring-boot:run
포트 : 8085 

```

## DDD 의 적용

- Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 데이터 접근 어댑터를 개발하였는가

MSA 모델링 도구 ( MSA Easy .io )를 사용하여 도출된 핵심 어그리게이트를 Entity로 선언하였다. 
=> 주문(order), 결제(payment), 주문관리(ordermanagement), 배송(delivery), 상품(item)

아래 코드는 주문 Entity에 대한 구현내용이다. 
```
package flowerdelivery;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;

@Entity
@Table(name="Order_table")
public class Order {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private String itemName;
    private Integer qty;
    private Long itemPrice;
    private String storeName;
    private String userName;

    @PostPersist
    public void onPostPersist(){
        Ordered ordered = new Ordered();
        BeanUtils.copyProperties(this, ordered);
        ordered.publishAfterCommit();

        //Following code causes dependency to external APIs
        // it is NOT A GOOD PRACTICE. instead, Event-Policy mapping is recommended.

        flowerdelivery.external.Payment payment = new flowerdelivery.external.Payment();
        // mappings goes here
        OrderApplication.applicationContext.getBean(flowerdelivery.external.PaymentService.class)
            .pay(payment);
    }

    @PreRemove
    public void onPreRemove(){
        OrderCancelled orderCancelled = new OrderCancelled();
        BeanUtils.copyProperties(this, orderCancelled);
        orderCancelled.publishAfterCommit();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }
    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }
    public Long getItemPrice() {
        return itemPrice;
    }

    public void setItemPrice(Long itemPrice) {
        this.itemPrice = itemPrice;
    }
    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
```
spring Data REST 의 RestRepository 를 적용하여 JPA를 통해 별도 처리 없이 다양한 데이터 소스 유형을 활용가능하도록 하였으며,
RDB 설정에 맞도록 Order Entity에 @Table, @Id 어노테이션을 표시하였다.

OrderReository.java 구현 내용
```
package flowerdelivery;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="orders", path="orders")
public interface OrderRepository extends PagingAndSortingRepository<Order, Long>{
}
```

Order.java 
```

@Entity
@Table(name="Order_table")
public class Order {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;

```

- [헥사고날 아키텍처] REST Inbound adaptor 이외에 gRPC 등의 Inbound Adaptor 를 추가함에 있어서 도메인 모델의 손상을 주지 않고 새로운 프로토콜에 기존 구현체를 적응시킬 수 있는가?

"미구현"


- 분석단계에서의 유비쿼터스 랭귀지 (업무현장에서 쓰는 용어) 를 사용하여 소스코드가 서술되었는가?

업무현장,현업에사 사용하는 용어(유비쿼터스 랭귀지)를 활용하여 모델링하였으며
, 한글사용시의 구동오류Case를 방지하기 위해 한글을 영문화 하여 구현하였다. 
(Maven pom.xml, Kafka의 topic id, FeignClient 의 서비스 id 등은 한글로 식별자를 사용하는 경우 오류가 발생함)


- 적용 후 REST API 의 테스트
```
주문 테스트
C:\workspace\flowerdelivery>http POST http://localhost:8088/orders storeName=KJSHOP itemName="장미 한바구니" qty=1 itemPrice=50000 userName=LKJ "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzY29wZSI6WyJyZWFkIiwid3JpdGUiLCJ0cnVzdCJdLCJjb21wYW55IjoiVWVuZ2luZSIsImV4cCI6MTYyMTg1OTQ1NywiYXV0aG9yaXRpZXMiOlsiUk9MRV9UUlVTVEVEX0NMSUVOVCIsIlJPTEVfQ0xJRU5UIl0sImp0aSI6ImlGZGswclgrR21TUVErN2xNS3ZWVGhtZFUxOD0iLCJjbGllbnRfaWQiOiJ1ZW5naW5lLWNsaWVudCJ9.DdilwqGMzcVOvWg69oDcqteM3tk1W2laMDc_sdz8YHJcfD-ZIJG5N4w_pGbxpypTZSz5YlAExJiJpUYtq3dPHnWTC0L2H2BRdredFO62no43vA3QoPDtiXgdOf7BqOzpMCQs1mMY4NqteoaKiD8aE-jG64-hOPSRx_VxZJ1MKezH9g-bA89Ptqaw0Rkuw9j5LuHqTVh0NANG58hfg0HAN3Y73RWnvBHPa2jcAGJL8lu1VarIujeatBHEOsXWVBBydlft2zol3vBvZBaGRJfW7Jt8vCyjqEfIShmQf0WGvXWwlX8XH1Q77JL617_Lxzjz-3uiDsLg-kN5U2TaoVUijQ"
HTTP/1.1 201 Created
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Content-Type: application/json;charset=UTF-8
Date: Sun, 23 May 2021 14:31:12 GMT
Expires: 0
Location: http://localhost:8081/orders/4
Pragma: no-cache
Referrer-Policy: no-referrer
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1 ; mode=block
transfer-encoding: chunked

{
    "_links": {
        "order": {
            "href": "http://localhost:8081/orders/4"
        },
        "self": {
            "href": "http://localhost:8081/orders/4"
        }
    },
    "itemName": "장미 한바구니",
    "itemPrice": 50000,
    "qty": 1,
    "storeName": "KJSHOP",
    "userName": "LKJ"
}

주문내역 조회 
C:\workspace\flowerdelivery>http GET localhost:8088/orders/4 "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzY29wZSI6WyJyZWFkIiwid3JpdGUiLCJ0cnVzdCJdLCJjb21wYW55IjoiVWVuZ2luZSIsImV4cCI6MTYyMTg1OTQ1NywiYXV0aG9yaXRpZXMiOlsiUk9MRV9UUlVTVEVEX0NMSUVOVCIsIlJPTEVfQ0xJRU5UIl0sImp0aSI6ImlGZGswclgrR21TUVErN2xNS3ZWVGhtZFUxOD0iLCJjbGllbnRfaWQiOiJ1ZW5naW5lLWNsaWVudCJ9.DdilwqGMzcVOvWg69oDcqteM3tk1W2laMDc_sdz8YHJcfD-ZIJG5N4w_pGbxpypTZSz5YlAExJiJpUYtq3dPHnWTC0L2H2BRdredFO62no43vA3QoPDtiXgdOf7BqOzpMCQs1mMY4NqteoaKiD8aE-jG64-hOPSRx_VxZJ1MKezH9g-bA89Ptqaw0Rkuw9j5LuHqTVh0NANG58hfg0HAN3Y73RWnvBHPa2jcAGJL8lu1VarIujeatBHEOsXWVBBydlft2zol3vBvZBaGRJfW7Jt8vCyjqEfIShmQf0WGvXWwlX8XH1Q77JL617_Lxzjz-3uiDsLg-kN5U2TaoVUijQ"
HTTP/1.1 200 OK
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Content-Type: application/hal+json;charset=UTF-8
Date: Sun, 23 May 2021 14:32:16 GMT
Expires: 0
Pragma: no-cache
Referrer-Policy: no-referrer
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1 ; mode=block
transfer-encoding: chunked

{
    "_links": {
        "order": {
            "href": "http://localhost:8081/orders/4"
        },
        "self": {
            "href": "http://localhost:8081/orders/4"
        }
    },
    "itemName": "장미 한바구니",
    "itemPrice": 50000,
    "qty": 1,
    "storeName": "KJSHOP",
    "userName": "LKJ"
}
```


## 동기식 호출 과 Fallback 처리

주문 시 주문과 결제처리를 동기식으로 처리하는 요구사항이 있다. 
호출 프로토콜은 이미 앞서 Rest Repository 에 의해 노출되어있는 REST 서비스를 FeignClient 를 이용하여 호출하도록 한다. 

주문(Order)서비스에서 결제서비스를 호출하기 위에 FeignClient 를 활용하여 Proxy를 구현하였다. 

PaymentService.java 
```
package flowerdelivery.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;


@FeignClient(name="payment", url="http://localhost:8082")
public interface PaymentService {

    @RequestMapping(method= RequestMethod.POST, path="/payments")
    public void pay(@RequestBody Payment payment);

}
```

주문 생성 직후(@PostPersist) 결제를 요청하도록 처리
Order.java Entity Class 내 추가
```
@PostPersist
    public void onPostPersist(){
        Ordered ordered = new Ordered();
        BeanUtils.copyProperties(this, ordered);
        ordered.publishAfterCommit();

        //Following code causes dependency to external APIs
        // it is NOT A GOOD PRACTICE. instead, Event-Policy mapping is recommended.

        flowerdelivery.external.Payment payment = new flowerdelivery.external.Payment();
        // mappings goes here
        OrderApplication.applicationContext.getBean(flowerdelivery.external.PaymentService.class)
            .pay(payment);
    }
```

동기식 호출에서는 호출 시간에 따른 타임 커플링이 발생하며, 결제 시스템이 장애가 나면 주문도 못받는다는 것을 확인:

```
Order서비스만 구동되어 있는 상태

꽃배달 주문 수행 시 오류 발생
C:\workspace\flowerdelivery>http POST http://localhost:8081/orders storeName="분당꽃배달" itemName="안개꽃한다발" qty=1 itemPrice=20000 userName="이기정" 
HTTP/1.1 500
Connection: close
Content-Type: application/json;charset=UTF-8
Date: Sun, 23 May 2021 15:18:41 GMT
Transfer-Encoding: chunked

{
    "error": "Internal Server Error",
    "message": "Could not commit JPA transaction; nested exception is javax.persistence.RollbackException: Error while committing the transaction",
    "path": "/orders",
    "status": 500,
    "timestamp": "2021-05-23T15:18:41.537+0000"
}


결제 서비스 구동 
C:\workspace\flowerdelivery\payment>mvn spring-boot:run

주문 재수행 - 정상처리됨을 확인
C:\workspace\flowerdelivery>http POST http://localhost:8081/orders storeName="분당꽃배달" itemName="안개꽃한다발" qty=1 itemPrice=20000 userName="이기정" 
HTTP/1.1 201
Content-Type: application/json;charset=UTF-8        
Date: Sun, 23 May 2021 15:20:37 GMT
Location: http://localhost:8081/orders/2
Transfer-Encoding: chunked

{
    "_links": {
        "order": {
            "href": "http://localhost:8081/orders/2"
        },
        "self": {
            "href": "http://localhost:8081/orders/2"
        }
    },
    "itemName": "안개꽃한다발",
    "itemPrice": 20000,
    "qty": 1,
    "storeName": "분당꽃배달",
    "userName": "이기정"
}

```

fallback 처리 

주문-결제 Req-Res 구조에 Spring Hystrix 를 사용하여 Fallback 기능을 구현 
FeignClient 내 Fallback 옵션과 Hystrix 설정 옵션으로 구현한다. 
먼저 PaymentService 에  feignClient fallback 옵션 및 Configuration 옵션을 추가하고 
fallback 클래스&메소드와 Configuration 클래스를 추가한다. 
(FeignClient 디펜던시에 nexflix Hystrix 관련 디펜던시가 추가되어 있어 별도 pom 수정은 필요하지 않는다.  ) 

```
package flowerdelivery.external;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import feign.Feign;
import feign.hystrix.HystrixFeign;
import feign.hystrix.SetterFactory;


@FeignClient(name="payment", url="http://localhost:8082", configuration=PaymentService.PaymentServiceConfiguration.class, fallback=PaymentService.PaymentServiceFallback.class)
public interface PaymentService {

    @RequestMapping(method= RequestMethod.POST, path="/payments")
    public void pay(@RequestBody Payment payment);

    @Component
    class PaymentServiceFallback implements PaymentService {

        @Override
        public void pay(Payment payment){
            System.out.println("★★★★★★★★★★★★★★PaymentServiceFallback works");   // fallback 메소드 작동 테스트
        }
    }

    @Component
    class PaymentServiceConfiguration {
        Feign.Builder feignBuilder(){
            SetterFactory setterFactory = (target, method) -> HystrixCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(target.name()))
            .andCommandKey(HystrixCommandKey.Factory.asKey(Feign.configKey(target.type(), method)))
            // 위는 groupKey와 commandKey 설정
            // 아래는 properties 설정
            .andCommandPropertiesDefaults(HystrixCommandProperties.defaultSetter()
                .withExecutionIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.SEMAPHORE)
                .withMetricsRollingStatisticalWindowInMilliseconds(10000) // 기준시간
                .withCircuitBreakerSleepWindowInMilliseconds(3000) // 서킷 열려있는 시간
                .withCircuitBreakerErrorThresholdPercentage(50)) // 에러 비율 기준 퍼센트
                ; // 최소 호출 횟수
            return HystrixFeign.builder().setterFactory(setterFactory);
        }        
    }
}
```

application.yml 파일에  feign.hystrix.enabled: true 로 활성화 시킨다. 

```
feign:
  hystrix:
    enabled: true
```


payment 서비스를 중지하고  주문 수행 시에는 오류가 발생하나, 위와 같이 fallback 기능을 활성화 후 수행 시에는 오류가 발생하지 않는다. 

payment 서비스 종료 후  fallback 기능 활성화 하지 않을 경우 아래와 같이 오류가 발생한다. 
```
C:\workspace\flowerdelivery>http POST http://localhost:8081/orders storeName="분당꽃배달" itemName="안개꽃한다발" qty=1 itemPrice=20000 userName="이기정" 
HTTP/1.1 500
Connection: close
Content-Type: application/json;charset=UTF-8
Date: Mon, 24 May 2021 10:18:40 GMT
Transfer-Encoding: chunked

{
    "error": "Internal Server Error",
    "message": "Could not commit JPA transaction; nested exception is javax.persistence.RollbackException: Error while committing the transaction",
    "path": "/orders",
    "status": 500,
    "timestamp": "2021-05-24T10:18:40.901+0000"
}
```

fallback 기능 활성화 시  payment서비스가 구동되지 않았지만 아래와 같이 오류문구가 발생하지 않는다.  

```
C:\workspace\flowerdelivery>http POST http://localhost:8081/orders storeName="분당꽃배달" itemName="안개꽃한다발" qty=1 itemPrice=20000 userName="이기정" 
HTTP/1.1 201
Content-Type: application/json;charset=UTF-8        
Date: Mon, 24 May 2021 10:19:42 GMT
Location: http://localhost:8081/orders/1
Transfer-Encoding: chunked

{
    "_links": {
        "order": {
            "href": "http://localhost:8081/orders/1"
        },
        "self": {
            "href": "http://localhost:8081/orders/1"
        }
    },
    "itemName": "안개꽃한다발",
    "itemPrice": 20000,
    "qty": 1,
    "storeName": "분당꽃배달",
    "userName": "이기정"
}
```

```
2021-05-24 19:19:41.219  INFO 36216 --- [nio-8081-exec-2] o.s.web.servlet.DispatcherServlet        : Completed initialization in 18 ms
Hibernate: 
    call next value for hibernate_sequence
Hibernate: 
    insert
    into
        order_table
        (item_name, item_price, qty, store_name, user_name, id)
    values
        (?, ?, ?, ?, ?, ?)
2021-05-24 19:19:41.366 TRACE 36216 --- [nio-8081-exec-2] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [VARCHAR] - [안개꽃한다발]
2021-05-24 19:19:41.367 TRACE 36216 --- [nio-8081-exec-2] o.h.type.descriptor.sql.BasicBinder      : binding parameter [2] as [BIGINT] - [20000]
2021-05-24 19:19:41.368 TRACE 36216 --- [nio-8081-exec-2] o.h.type.descriptor.sql.BasicBinder      : binding parameter [3] as [INTEGER] - [1]
2021-05-24 19:19:41.368 TRACE 36216 --- [nio-8081-exec-2] o.h.type.descriptor.sql.BasicBinder      : binding parameter [4] as [VARCHAR] - [분당꽃배달]
2021-05-24 19:19:41.369 TRACE 36216 --- [nio-8081-exec-2] o.h.type.descriptor.sql.BasicBinder      : binding parameter [5] as [VARCHAR] - [이기정]
2021-05-24 19:19:41.369 TRACE 36216 --- [nio-8081-exec-2] o.h.type.descriptor.sql.BasicBinder      : binding parameter [6] as [BIGINT] - [1]
2021-05-24 19:19:41.581 DEBUG 36216 --- [strix-payment-1] o.s.c.openfeign.support.SpringEncoder    : Writing [flowerdelivery.external.Payment@3ad63bc4] using [org.springframework.http.converter.json.MappingJackson2HttpMessageConverter@5ccd6bd4]
★★★★★★★★★★★★★★PaymentServiceFallback works   <<=  // fallback 메소드 작동 테스트
2021-05-24 19:19:42.592 DEBUG 36216 --- [nio-8081-exec-2] o.s.c.s.m.DirectWithAttributesChannel    : preSend on channel 'event-out', message: GenericMessage [payload={"eventType":"Ordered","timestamp":"20210524191941","id":1,"storeName":"분당꽃배달","itemName":"안개꽃한다발","qty":1,"userName":"이기정","itemPrice":null,"orderStatus":null,"me":true}, headers={contentType=application/json, id=8aa33ad5-01a3-0212-c9b4-3dc2f8d2b1b6, timestamp=1621851582592}]
```
위와 같이 로그로 남긴 fallback 작동 메시지가 display 된다. 




## 비동기식호출

- 카프카를 이용하여 PubSub 으로 하나 이상의 서비스가 연동되었는가?

메시지 브로커로 카프카를 이용하였고,  결제 - 주문관리 서비스 간에서는  결제됨 이벤트를  / 주문관리 - 배달 서비스 간에는 데코레이션됨 이벤트 등을 Pub/Sub 관계로 구현하였다. 

아래는 결제됨 이벤트를 카프카를 통해 연계받는 코드 내용이다. 

Payment 서비스에서는 Post(생성)이벤트에서 Paid() 이벤트 발생시킴
```
public class Payment {
    @PostPersist
    public void onPostPersist(){
    		
    		Paid paid = new Paid();
    		BeanUtils.copyProperties(this, paid);
    		paid.publishAfterCommit();
    }
```

ordermanagement 서비스에서는 카프카 리스너를 통해 Paid 이벤트를 수신 받아서 이후 처리함
```
@Service
public class PolicyHandler{
	@StreamListener(KafkaProcessor.INPUT)
	    public void wheneverPaid_AcceptRequest(@Payload Paid paid){
		if(paid.isMe()){
		    System.out.println("##### listener AcceptRequest : " + paid.toJson());
		    System.out.println("paid 주문 발생");
		    System.out.println("주문 번호: "+ paid.getOrderId());
		    Ordermanagement ordermanagement= new Ordermanagement();

		    ordermanagement.setOrderId(paid.getOrderId());
		    ordermanagement.setOrdermanagementStatus("null");
		    ordermanagement.setPaymentStatus(paid.getPaymentStatus());
		    ordermanagement.setQty(paid.getQty());
		    ordermanagement.setStoreName(paid.getStoreName());
		    ordermanagement.setUserName(null);           
		    orderManagementRepository.save(ordermanagement);
		}
	    }
```

- Correlation-key: 각 이벤트 건 (메시지)가 어떠한 폴리시를 처리할때 어떤 건에 연결된 처리건인지를 구별하기 위한 Correlation-key 연결을 제대로 구현 하였는가?

MSAez 모델링 도구를 활용하여 각 서비스의 이벤트와 폴리시간의 연결을 pub/sub 점선으로 표현하였으며, 이를 코드 자동생성하여 Correlation -key 연결을 활용하였다. 


- Message Consumer 마이크로서비스가 장애상황에서 수신받지 못했던 기존 이벤트들을 다시 수신받아 처리하는가?

주문서비스 - Req/Res - 결제서비스 -  Pub/Sub Paid이벤트 -  주문관리서비스  구조에서 

주문관리 서비스를 중지하고 신규 주문 발생시 아래와 같이 정상처리 되며  카프카 큐에 저장되어 있다. 

```
C:\workspace\flowerdelivery>http POST http://localhost:8081/orders storeName=KJSHOP itemName="roses set" qty=1 itemPrice=50000 userName=LKJ
HTTP/1.1 201
Content-Type: application/json;charset=UTF-8
Date: Tue, 25 May 2021 01:40:32 GMT
Location: http://localhost:8081/orders/1
Transfer-Encoding: chunked

{
    "_links": {
        "order": {
            "href": "http://localhost:8081/orders/1"
        },
        "self": {
            "href": "http://localhost:8081/orders/1"
        }
    },
    "itemName": "roses set",
    "itemPrice": 50000,
    "qty": 1,
    "storeName": "KJSHOP",
    "userName": "LKJ"
}
```

카프카 

![image](https://user-images.githubusercontent.com/80744199/119427582-3802cc80-bd46-11eb-844b-ef2b74a14066.png)


이후 주문관리 서비스를 재구동 하면  카프카에 저장된 데이터를 확인하여 주문관리 데이터가 생성된다. 

```
kafka_receivedMessageKey=null, kafka_receivedPartitionId=0, contentType=application/json, kafka_receivedTopic=flowerdelivery, kafka_receivedTimestamp=1621906832138}]
##### listener AcceptRequest : {"eventType":"Paid","timestamp":"20210525104032","id":1,"orderId":1,"storeName":"KJSHOP","itemName":"roses set","qty":1,"paymentStatus":"paid","me":true}
paid 주문 발생
주문 번호: 1
Hibernate: 
    call next value for hibernate_sequence
Hibernate: 
    insert
    into
        ordermanagement_table
        (item_name, order_id, ordermanagement_status, payment_status, qty, store_name, user_name, id)
    values
        (?, ?, ?, ?, ?, ?, ?, ?)
2021-05-25 10:41:10.750 TRACE 26256 --- [container-0-C-1] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [VARCHAR] - [null]
2021-05-25 10:41:10.751 TRACE 26256 --- [container-0-C-1] o.h.type.descriptor.sql.BasicBinder      : binding parameter [2] as [BIGINT] - [1]
2021-05-25 10:41:10.751 TRACE 26256 --- [container-0-C-1] o.h.type.descriptor.sql.BasicBinder      : binding parameter [3] as [VARCHAR] - [null]
2021-05-25 10:41:10.751 TRACE 26256 --- [container-0-C-1] o.h.type.descriptor.sql.BasicBinder      : binding parameter [4] as [VARCHAR] - [paid]
2021-05-25 10:41:10.752 TRACE 26256 --- [container-0-C-1] o.h.type.descriptor.sql.BasicBinder      : binding parameter [5] as [INTEGER] - [1]
2021-05-25 10:41:10.752 TRACE 26256 --- [container-0-C-1] o.h.type.descriptor.sql.BasicBinder      : binding parameter [6] as [VARCHAR] - [KJSHOP]
2021-05-25 10:41:10.753 TRACE 26256 --- [container-0-C-1] o.h.type.descriptor.sql.BasicBinder      : binding parameter [7] as [VARCHAR] - [null]
2021-05-25 10:41:10.753 TRACE 26256 --- [container-0-C-1] o.h.type.descriptor.sql.BasicBinder      : binding parameter [8] as [BIGINT] - [1]
2021-05-25 10:41:10.755 DEBUG 26256 --- [container-0-C-1] o.s.c.s.b.StreamListenerMessageHandler   : handler 'org.springframework.cloud.stream.binding.StreamListenerMessageHandler@6d3bd644' produced no reply 
for request Message: GenericMessage [payload=byte[153], headers={kafka_offset=76, scst_nativeHeadersPresent=true, kafka_consumer=org.apache.kafka.clients.consumer.KafkaConsumer@169bad86, deliveryAttempt=1, kafka_timestampType=CREATE_TIME, kafka_receivedMessageKey=null, kafka_receivedPartitionId=0, contentType=application/json, kafka_receivedTopic=flowerdelivery, kafka_receivedTimestamp=1621906832138}]
```


- Scaling-out: Message Consumer 마이크로서비스의 Replica 를 추가했을때 중복없이 이벤트를 수신할 수 있는가

주문관리 서비스를 1개에서 => 3개로 노드를 추가한다. 
포트 중복을 방지하기 위해  8083, 8093, 8094 로 변경하여 구동하였으며 

추가 노드 구동시에는 카프카 컨슈머 그룹에서 파티션이 재할당된다.  

파티션 사이즈가 1이라서 
기존 구동한 8083 노드는 파티션을 할당받으며 신규 추가된 노드는 파티션 할당을 받지 못한다. 

파티션 할당받지못한 2번 3번 노드 
```
2021-05-25 10:54:45.411  INFO 37684 --- [container-0-C-1] o.a.k.c.c.internals.ConsumerCoordinator  : [Consumer clientId=consumer-3, groupId=ordermanagement] Revoking previously assigned partitions []
2021-05-25 10:54:45.411  INFO 37684 --- [container-0-C-1] o.s.c.s.b.k.KafkaMessageChannelBinder$1  : partitions revoked: []
2021-05-25 10:54:45.412  INFO 37684 --- [container-0-C-1] o.a.k.c.c.internals.AbstractCoordinator  : [Consumer clientId=consumer-3, groupId=ordermanagement] (Re-)joining group
2021-05-25 10:54:45.416  INFO 37684 --- [container-0-C-1] o.a.k.c.c.internals.AbstractCoordinator  : [Consumer clientId=consumer-3, groupId=ordermanagement] Successfully joined group with generation 12       
2021-05-25 10:54:45.416  INFO 37684 --- [container-0-C-1] o.a.k.c.c.internals.ConsumerCoordinator  : [Consumer clientId=consumer-3, groupId=ordermanagement] Setting newly assigned partitions []
2021-05-25 10:54:45.417  INFO 37684 --- [container-0-C-1] o.s.c.s.b.k.KafkaMessageChannelBinder$1  : partitions assigned: []
```

3개가 구동된 상태에서 신규 주문을 추가한 경우 

3개 중 1개 노드만 이벤트를 받아서 처리하고 나머지 2개 노드는 로그가 변화가 없다. 

이벤트를 수신한 1번 노드 로그 
```
##### listener AcceptRequest : {"eventType":"Paid","timestamp":"20210525105421","id":14,"orderId":27,"storeName":"KJSHOP","itemName":"roses set","qty":1,"paymentStatus":"paid","me":true}
paid 주문 발생
주문 번호: 27
Hibernate: 
    call next value for hibernate_sequence
Hibernate:
    insert
    into
        ordermanagement_table
        (item_name, order_id, ordermanagement_status, payment_status, qty, store_name, user_name, id)
    values
        (?, ?, ?, ?, ?, ?, ?, ?)
2021-05-25 10:54:21.599 TRACE 26256 --- [container-0-C-1] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [VARCHAR] - [null]
2021-05-25 10:54:21.599 TRACE 26256 --- [container-0-C-1] o.h.type.descriptor.sql.BasicBinder      : binding parameter [2] as [BIGINT] - [27]
2021-05-25 10:54:21.599 TRACE 26256 --- [container-0-C-1] o.h.type.descriptor.sql.BasicBinder      : binding parameter [3] as [VARCHAR] - [null]
2021-05-25 10:54:21.599 TRACE 26256 --- [container-0-C-1] o.h.type.descriptor.sql.BasicBinder      : binding parameter [4] as [VARCHAR] - [paid]
2021-05-25 10:54:21.600 TRACE 26256 --- [container-0-C-1] o.h.type.descriptor.sql.BasicBinder      : binding parameter [5] as [INTEGER] - [1]
2021-05-25 10:54:21.600 TRACE 26256 --- [container-0-C-1] o.h.type.descriptor.sql.BasicBinder      : binding parameter [6] as [VARCHAR] - [KJSHOP]
2021-05-25 10:54:21.600 TRACE 26256 --- [container-0-C-1] o.h.type.descriptor.sql.BasicBinder      : binding parameter [7] as [VARCHAR] - [null]
2021-05-25 10:54:21.600 TRACE 26256 --- [container-0-C-1] o.h.type.descriptor.sql.BasicBinder      : binding parameter [8] as [BIGINT] - [14]
```

변화 없는 2번 3번 노드 로그 
```
로그변화 없음 
```

1번 노드를 중지할 경우  2번 노드가 파티션을 할당받아서 이후의 이벤트를 수신 처리한다. 

2번 노드가 파티션을 할당받은 로그
```
2021-05-25 10:54:45.411  INFO 28684 --- [container-0-C-1] o.a.k.c.c.internals.ConsumerCoordinator  : [Consumer clientId=consumer-3, groupId=ordermanagement] Revoking previously assigned partitions []
2021-05-25 10:54:45.411  INFO 28684 --- [container-0-C-1] o.s.c.s.b.k.KafkaMessageChannelBinder$1  : partitions revoked: []
2021-05-25 10:54:45.411  INFO 28684 --- [container-0-C-1] o.a.k.c.c.internals.AbstractCoordinator  : [Consumer clientId=consumer-3, groupId=ordermanagement] (Re-)joining group
2021-05-25 10:54:45.416  INFO 28684 --- [container-0-C-1] o.a.k.c.c.internals.AbstractCoordinator  : [Consumer clientId=consumer-3, groupId=ordermanagement] Successfully joined group with generation 12       
2021-05-25 10:54:45.417  INFO 28684 --- [container-0-C-1] o.a.k.c.c.internals.ConsumerCoordinator  : [Consumer clientId=consumer-3, groupId=ordermanagement] Setting newly assigned partitions [flowerdelivery-0]
2021-05-25 10:54:45.421  INFO 28684 --- [container-0-C-1] o.s.c.s.b.k.KafkaMessageChannelBinder$1  : partitions assigned: [flowerdelivery-0]
```



- CQRS: Materialized View 를 구현하여, 타 마이크로서비스의 데이터 원본에 접근없이(Composite 서비스나 조인SQL 등 없이) 도 내 서비스의 화면 구성과 잦은 조회가 가능한가?

![image](https://user-images.githubusercontent.com/80744199/119308095-96c53900-bca7-11eb-840c-4d12883e0ec7.png)

주문 / 결제 / 주문관리 / 배송 서비스의 전체 현황 및 상태 조회를 제공하기 위해 주문 서비스 내에 MyPage View를 모델링 하였다. 

MyPage View 의 어트리뷰트는 다음과 같으며 

![image](https://user-images.githubusercontent.com/80744199/119308246-c70cd780-bca7-11eb-9d5e-0da2f2d3755d.png)


주문이 생성될때 MyPage 데이터도 생성되어 "결제완료됨, 주문취소됨, 강제취소됨, 등록취소됨, 주문접수됨, 꽃장식완료됨, 주문거절됨, 배달취소됨, 배달출발함, 배달완료됨" 의 이벤트에 따라 주문상태, 배송상태를 업데이트하는 모델링을 진행하였으며,

MSAEZ 모델링 도구 내 View CQRS 설정 패널 샘플 

![image](https://user-images.githubusercontent.com/80744199/119308604-35519a00-bca8-11eb-8562-d1642e9cc6ba.png)

자동생성된 소스 샘플은 아래와 같다. 

MyPage.java   엔티티 클래스 
```
package flowerdelivery;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name="MyPage_table")
public class MyPage {

        @Id
        @GeneratedValue(strategy=GenerationType.AUTO)
        private Long id;
        private String storeName;
        private String itemName;
        private Integer orderQty;
        private Integer itemPrice;
        private String orderStatus;
        private String deliveryStatus;
        private Long orderId;
        private String userName;


        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
        public String getStoreName() {
            return storeName;
        }

        public void setStoreName(String storeName) {
            this.storeName = storeName;
        }
        public String getItemName() {
            return itemName;
        }

        public void setItemName(String itemName) {
            this.itemName = itemName;
        }
        public Integer getOrderQty() {
            return orderQty;
        }

        public void setOrderQty(Integer orderQty) {
            this.orderQty = orderQty;
        }
        public Integer getItemPrice() {
            return itemPrice;
        }

        public void setItemPrice(Integer itemPrice) {
            this.itemPrice = itemPrice;
        }
        public String getOrderStatus() {
            return orderStatus;
        }

        public void setOrderStatus(String orderStatus) {
            this.orderStatus = orderStatus;
        }
        public String getDeliveryStatus() {
            return deliveryStatus;
        }

        public void setDeliveryStatus(String deliveryStatus) {
            this.deliveryStatus = deliveryStatus;
        }
        public Long getOrderId() {
            return orderId;
        }

        public void setOrderId(Long orderId) {
            this.orderId = orderId;
        }
        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

}

```

MyPageRepository.java    퍼시스턴스 
```
package flowerdelivery;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MyPageRepository extends CrudRepository<MyPage, Long> {

    List<MyPage> findByOrderId(Long orderId);

    void deleteByOrderId(Long orderId);

}
```

MyPageViewHandler.java 
View 핸들러에는 이벤트 수신 처리부가 있으며  생성 및 변경에 대한 이벤트 코드를 첨부한다. 

주문생성 시 처리되는 이벤트 
```
@StreamListener(KafkaProcessor.INPUT)
    public void whenOrdered_then_CREATE_1 (@Payload Ordered ordered) {
        try {

            //if (!ordered.validate()) return;
            System.out.println("Order Created");
            if(ordered.isMe()){
                // view 객체 생성
                MyPage myPage = new MyPage();
                // view 객체에 이벤트의 Value 를 set 함
                myPage.setOrderId(ordered.getId());
                myPage.setStoreName(ordered.getStoreName());
                myPage.setItemName(ordered.getItemName());
                myPage.setOrderQty(ordered.getQty());
                myPage.setItemPrice(ordered.getItemPrice());
                myPage.setUserName(ordered.getUserName());
                // view 레파지 토리에 save
                myPageRepository.save(myPage);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
```

업데이트 이벤트 
```
@StreamListener(KafkaProcessor.INPUT)
    public void whenPaid_then_UPDATE_1(@Payload Paid paid) {
        try {
            //if (!paid.validate()) return;
                // view 객체 조회

                if(paid.isMe()){
                    Optional<MyPage> myPageOptional = myPageRepository.findById(paid.getOrderId());
                    if( myPageOptional.isPresent()) {
                        MyPage myPage = myPageOptional.get();
                        // view 객체에 이벤트의 eventDirectValue 를 set 함
                            myPage.setOrderStatus(paid.getPaymentStatus());
                        // view 레파지 토리에 save
                        myPageRepository.save(myPage);
                    }

                }
            
        }catch (Exception e){
            e.printStackTrace();
        }
    }
```

CQRS에 대한 테스트는 아래와 같다. 

MyPage  CQRS처리를 위해  주문, 결제, 주문관리, 배송과 별개로  조회를 위한 MyPage_table  테이블이 생성되어 있으며, 

주문생성 시 아래와 같이 정상 등록이 되며 
```
C:\workspace\flowerdelivery>http POST http://localhost:8081/orders storeName=KJSHOP itemName="장미 한바구니" qty=1 itemPrice=50000 userName=LKJ
HTTP/1.1 201
Content-Type: application/json;charset=UTF-8        
Date: Mon, 24 May 2021 07:04:31 GMT
Location: http://localhost:8081/orders/1
Transfer-Encoding: chunked

{
    "_links": {
        "order": {
            "href": "http://localhost:8081/orders/1"
        },
        "self": {
            "href": "http://localhost:8081/orders/1"
        }
    },
    "itemName": "장미 한바구니",
    "itemPrice": 50000,
    "qty": 1,
    "storeName": "KJSHOP",
    "userName": "LKJ"
}

``` 

MyPage CQRS 결과는 아래와 같다. 
```
C:\workspace\flowerdelivery>http http://localhost:8081/myPages
HTTP/1.1 200 
Content-Type: application/hal+json;charset=UTF-8
Date: Mon, 24 May 2021 07:05:18 GMT
Transfer-Encoding: chunked

{
    "_embedded": {
        "myPages": [
            {
                "_links": {
                    "myPage": {
                        "href": "http://localhost:8081/myPages/2"
                    },
                    "self": {
                        "href": "http://localhost:8081/myPages/2"
                    }
                },
                "deliveryStatus": null,
                "itemName": "장미 한바구니",
                "itemPrice": null,
                "orderId": 1,
                "orderQty": 1,
                "orderStatus": null,
                "storeName": "KJSHOP",
                "userName": "LKJ"
            }
        ]
    },
    "_links": {
        "profile": {
            "href": "http://localhost:8081/profile/myPages"
        },
        "search": {
            "href": "http://localhost:8081/myPages/search"
        },
        "self": {
            "href": "http://localhost:8081/myPages"
        }
    }
}
```


주문 취소 시에는 
```
C:\workspace\flowerdelivery>http DELETE http://localhost:8081/orders/1
HTTP/1.1 204 
Date: Mon, 24 May 2021 07:05:57 GMT
```


MyPage CQRS도 주문취소 이벤트에 대한 처리( 주문상태 : OrderCancelled 로 변경) 를 통해 같이 변경된다. 
```
C:\workspace\flowerdelivery>http http://localhost:8081/myPages
HTTP/1.1 200 
Content-Type: application/hal+json;charset=UTF-8
Date: Mon, 24 May 2021 07:10:40 GMT
Transfer-Encoding: chunked

{
    "_embedded": {
        "myPages": [
            {
                "_links": {
                    "myPage": {
                        "href": "http://localhost:8081/myPages/2"
                    },
                    "self": {
                        "href": "http://localhost:8081/myPages/2"
                    }
                },
                "deliveryStatus": null,
                "itemName": "장미 한바구니",
                "itemPrice": null,
                "orderId": 1,
                "orderQty": 1,
                "orderStatus": "OrderCancelled",
                "storeName": "KJSHOP",
                "userName": "LKJ"
            }
        ]
    },
    "_links": {
        "profile": {
            "href": "http://localhost:8081/profile/myPages"
        },
        "search": {
            "href": "http://localhost:8081/myPages/search"
        },
        "self": {
            "href": "http://localhost:8081/myPages"
        }
    }
}
```
신규 아이템 서비스가 추가되어 Menu CQRS를 추가함

![image](https://user-images.githubusercontent.com/80744199/121294062-b905ba80-c927-11eb-8037-655fd6d47b66.png)



View Page 에 대한 설정

![image](https://user-images.githubusercontent.com/80744199/121294122-cde24e00-c927-11eb-8ab9-cc51e12bc3b5.png)



## 폴리글랏 퍼시스턴스

배송 서비스(delivery)는 실시간 배송위치 추적 등 추후 지도(GIS) 기반 서비스의 확장까지 고려하여 데이터베이스를 선정하려고 한다. 
postgres는 공간(Spatial)부분에 상당한 강점과 다양한 레퍼런스가 있어서 적합하다고 판단되어  배송(delivery)서비스의 DB는 자동생성된 DB설정인 H2에서 postgreSQL로 변경하려고 한다. 

먼저, AWS에 postgreSQL 을 프리티어로 생성한다. 

AWS > RDS > 데이터베이스 생성

![image](https://user-images.githubusercontent.com/80744199/119250376-ad518e80-bbda-11eb-852e-6f64e76dfdad.png)

생성된 모습 

![image](https://user-images.githubusercontent.com/80744199/119250409-e12cb400-bbda-11eb-88c7-58725f0b603e.png)

접속 허용을 위해 보안그룹을 추가하고,  인바운드 규칙에 모든TCP를 허용한다. 

![image](https://user-images.githubusercontent.com/80744199/119250559-cdce1880-bbdb-11eb-8b23-fe0a668c524d.png)

PgAdmin을 통해 접속가능 확인

![image](https://user-images.githubusercontent.com/80744199/119250566-e0485200-bbdb-11eb-9ca5-365e3dad00a0.png)


delivery 서비스의 postgresql dependency 추가 

기존 h2 

![image](https://user-images.githubusercontent.com/80744199/119251064-5e5a2800-bbdf-11eb-8b56-27c8fc3e4863.png)

변경 postgreSQL

![image](https://user-images.githubusercontent.com/80744199/119251052-50a4a280-bbdf-11eb-8e20-e5a7ada61ff0.png)


delivery 서비스의 application.yml 수정 

기존 설정  (H2 DB) 

![image](https://user-images.githubusercontent.com/80744199/119251098-9f523c80-bbdf-11eb-9215-da643b6bafc3.png)
 
변경 설정 ( postgreSQL DB ) 

![image](https://user-images.githubusercontent.com/80744199/119251089-93667a80-bbdf-11eb-8327-aa8d776f2cbd.png)

 

RDB -> RDB로 변경하여 Java Source 부분에는 추가 변경이 필요치 않음

mvn spring-boot:run 으로 구동하여 배송서비스(delivery) 관련 테이블이 postgres에 생성된 모습

![image](https://user-images.githubusercontent.com/80744199/119254017-f7dd0600-bbee-11eb-8817-83eda34bcf13.png)



## 폴리글랏 프로그래밍

"미구현"


## API게이트웨이

- API GW를 통하여 마이크로 서비스들의 집입점을 통일할 수 있는가?

MSAEZ 모델링 도구를 통해 자동생성된 gateway 를 구동하고, spring.cloud.gateway.routes 정보를 설정하여 마이크로 서비스의 진입점을 통일한다. 

gateway 서비스의 application.yml 파일 

```
  cloud:
    gateway:
      routes:
        - id: order
          uri: http://localhost:8081
          predicates:
            - Path=/orders/**, /menus/**/myPages/**
        - id: payment
          uri: http://localhost:8082
          predicates:
            - Path=/payments/** 
        - id: ordermanagement
          uri: http://localhost:8083
          predicates:
            - Path=/ordermanagements/**, /orderStatuses/**
        - id: delivery
          uri: http://localhost:8084
          predicates:
            - Path=/deliveries/**, /deliverystatuses/**
```

게이트웨이 포트를 활용하여 각 서비스로 접근 가능한지 확인 

게이트웨이포트 8088 을 통해서    8081포트에 서비스하고 있는 주문(Order)서비스를 접근함 

```
C:\workspace\flowerdelivery>http GET localhost:8088/orders/1 "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzY29wZSI6WyJyZWFkIiwid3JpdGUiLCJ0cnVzdCJdLCJjb21wYW55IjoiVWVuZ2luZSIsImV4cCI6MTYyMTg1OTQ1NywiYXV0aG9yaXRpZXMiOlsiUk9MRV9UUlVTVEVEX0NMSUVOVCIsIlJPTEVfQ0xJRU5UIl0sImp0aSI6ImlGZGswclgrR21TUVErN2xNS3ZWVGhtZFUxOD0iLCJjbGllbnRfaWQiOiJ1ZW5naW5lLWNsaWVudCJ9.DdilwqGMzcVOvWg69oDcqteM3tk1W2laMDc_sdz8YHJcfD-ZIJG5N4w_pGbxpypTZSz5YlAExJiJpUYtq3dPHnWTC0L2H2BRdredFO62no43vA3QoPDtiXgdOf7BqOzpMCQs1mMY4NqteoaKiD8aE-jG64-hOPSRx_VxZJ1MKezH9g-bA89Ptqaw0Rkuw9j5LuHqTVh0NANG58hfg0HAN3Y73RWnvBHPa2jcAGJL8lu1VarIujeatBHEOsXWVBBydlft2zol3vBvZBaGRJfW7Jt8vCyjqEfIShmQf0WGvXWwlX8XH1Q77JL617_Lxzjz-3uiDsLg-kN5U2TaoVUijQ"
HTTP/1.1 200 OK
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Content-Type: application/hal+json;charset=UTF-8
Date: Sun, 23 May 2021 14:06:59 GMT
Expires: 0
Pragma: no-cache
Referrer-Policy: no-referrer
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1 ; mode=block
transfer-encoding: chunked

{
    "_links": {
        "order": {
            "href": "http://localhost:8081/orders/1"
        },
        "self": {
            "href": "http://localhost:8081/orders/1"
        }
    },
    "itemName": "cup001",
    "itemPrice": 1000,
    "qty": 10,
    "storeName": "KJSHOP",
    "userName": "LKJ"
}
```

- 게이트웨이와 인증서버(OAuth), JWT 토큰 인증을 통하여 마이크로서비스들을 보호할 수 있는가?

먼저 게이트웨이에서 JWT 인증을 하기 위에  Spring Security 관련 디펜던시를 추가한다. 
gateway 서비스의 pom.xml 파일에 내용 추가
```
<!-- Add spring security -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-webflux</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-security</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.security</groupId>
			<artifactId>spring-security-oauth2-client</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.security</groupId>
			<artifactId>spring-security-oauth2-jose</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.security</groupId>
			<artifactId>spring-security-oauth2-resource-server</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.security.oauth.boot</groupId>
			<artifactId>spring-security-oauth2-autoconfigure</artifactId>
		</dependency>
```

application.yml 파일에  spring security jwt 설정을 추가한다. 
(인증(OAUTH)서비스는 8090 포트로 서비스 예정이다.)
```
spring:
  profiles: default
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: http://localhost:8090/.well-known/jwks.json
```

JWT ResourceServerCofiguration.java 추가 
```
package com.example;

import org.springframework.cloud.gateway.config.GlobalCorsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebFluxSecurity
public class ResourceServerConfiguration {

    @Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) throws Exception {

        http
                .cors().and()
                .csrf().disable()
                .authorizeExchange()
                //.pathMatchers("/orders/**","/deliveries/**","/oauth/**","/login/**","/payments/**","/ordermanagement/**").permitAll()
                .pathMatchers("/oauth/**").permitAll()
                .anyExchange().authenticated()
                .and()
                .oauth2ResourceServer()
                .jwt()
                ;

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            GlobalCorsProperties globalCorsProperties) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        globalCorsProperties.getCorsConfigurations()
                .forEach(source::registerCorsConfiguration);
        return source;
    }
}
```
event-storming/oauth 서비스를 flowerdelivery 프로젝트에 추가한다. 

![image](https://user-images.githubusercontent.com/80744199/119264224-312c6a80-bc1d-11eb-92e3-fbb0eadc5cb0.png)

인증(oauth)서비스를 구동시킨다.
c:\workspace\flowdelivery\oauth> mvn spring-boot:run 

게이트웨이 내 Spring Security 설정에 따라서 인증토큰이 없는 서비스 호출은 인증되지 않은 호출로 오류가 발생한다. 
```
C:\workspace\flowerdelivery>http GET http://localhost:8088/orders/1
HTTP/1.1 401 Unauthorized
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Expires: 0
Pragma: no-cache
Referrer-Policy: no-referrer
WWW-Authenticate: Bearer
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1 ; mode=block
content-length: 0
```

인증을 위해 client_credintials 방식으로  토큰을 요청한다. 
```
C:\workspace\flowerdelivery>http --form POST localhost:8090/oauth/token "Authorization: Basic dWVuZ2luZS1jbGllbnQ6dWVuZ2luZS1zZWNyZXQ=" grant_type=client_credentials
HTTP/1.1 200 
Cache-Control: no-store
Connection: keep-alive
Content-Type: application/json;charset=UTF-8
Date: Sun, 23 May 2021 14:25:55 GMT
Keep-Alive: timeout=60
Pragma: no-cache
Transfer-Encoding: chunked
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block

{
    "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzY29wZSI6WyJyZWFkIiwid3JpdGUiLCJ0cnVzdCJdLCJjb21wYW55IjoiVWVuZ2luZSIsImV4cCI6MTYyMTg2NjM1NSwiYXV0aG9yaXRpZXMiOlsiUk9MRV9UUlVTVEVEX0NMSUVOVCIsIlJPTEVfQ0xJRU5UIl0sImp0aSI6Ikt0RUhTSk5xVVVtSzJYSU92bVpQanYydVJmMD0iLCJjbGllbnRfaWQiOiJ1ZW5naW5lLWNsaWVudCJ9.Iah6pc7kwSY4uyjy40AJlt43vp4sLoDfnjaxhK4zr-2r30BOaqPasU8DOMWrl99BM8AlVmwwesuaxKdcJOGc89R_TrmqHbWAe3_enHXlTr3JsiXzQWzyNTTtgxNFjoP0Tn-wtUg_shirmn8UTR9DQN5N1uJk_3TswQVTPoqz-11SDepIvkT5fbdNqXAJ7rcpJXJzKv89Cr6YagU3Wp-KqhtA0-QSi3Z_qBaWzQlYjta1CqKVZE9xciCWssEFtVOpRr7Tv2vuaIFHDBE_hd7fg3wXRJ55XYl0kkaLtHqN2RW4ZqbsxAT-HoW4lJFO8jRtEaElQclzZqcJ5mbLcssiSw",
```

얻은 위 access_token 정보를 활용하여 서비스를 호출 시 정상적으로 동작한다. 

```
C:\workspace\flowerdelivery>http GET localhost:8088/orders/1 "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzY29wZSI6WyJyZWFkIiwid3JpdGUiLCJ0cnVzdCJdLCJjb21wYW55IjoiVWVuZ2luZSIsImV4cCI6MTYyMTg1OTQ1NywiYXV0aG9yaXRpZXMiOlsiUk9MRV9UUlVTVEVEX0NMSUVOVCIsIlJPTEVfQ0xJRU5UIl0sImp0aSI6ImlGZGswclgrR21TUVErN2xNS3ZWVGhtZFUxOD0iLCJjbGllbnRfaWQiOiJ1ZW5naW5lLWNsaWVudCJ9.DdilwqGMzcVOvWg69oDcqteM3tk1W2laMDc_sdz8YHJcfD-ZIJG5N4w_pGbxpypTZSz5YlAExJiJpUYtq3dPHnWTC0L2H2BRdredFO62no43vA3QoPDtiXgdOf7BqOzpMCQs1mMY4NqteoaKiD8aE-jG64-hOPSRx_VxZJ1MKezH9g-bA89Ptqaw0Rkuw9j5LuHqTVh0NANG58hfg0HAN3Y73RWnvBHPa2jcAGJL8lu1VarIujeatBHEOsXWVBBydlft2zol3vBvZBaGRJfW7Jt8vCyjqEfIShmQf0WGvXWwlX8XH1Q77JL617_Lxzjz-3uiDsLg-kN5U2TaoVUijQ"
HTTP/1.1 200 OK
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Content-Type: application/hal+json;charset=UTF-8
Date: Sun, 23 May 2021 14:27:42 GMT
Expires: 0
Pragma: no-cache
Referrer-Policy: no-referrer
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1 ; mode=block
transfer-encoding: chunked

{
    "_links": {
        "order": {
            "href": "http://localhost:8081/orders/1"
        },
        "self": {
            "href": "http://localhost:8081/orders/1"
        }
    },
    "itemName": "cup001",
    "itemPrice": 1000,
    "qty": 10,
    "storeName": "KJSHOP",
    "userName": "LKJ"
}
```

## SAGA패턴적용

SAGA 패턴은 각 서비스의 트랜잭션은 단일 서비스 내의 데이터를 갱신하는 일종의 로컬 트랜잭션 방법이고 서비스의 트랜잭션이 완료 후에 다음 서비스가 트리거 되어, 트랜잭션을 실행하는 방법입니다.

현재 FlowerDelivery 시스템에도 SAGA 패턴에 맞추어서 작성되어 있다.

**SAGA 패턴에 맞춘 트랜잭션 실행**

![image](https://user-images.githubusercontent.com/44644430/119428043-253cc780-bd47-11eb-9ed4-06e5321a7f5c.png)

현재 FlowerDelivery 시스템은 SAGA 패턴에 맞추어서 Order 서비스의 Order생성이 완료되면 Payment 서비스를 트리거하게 되어 paymentStatus를 paid 상태로 업데이트하여
OrderManagement 서비스에서 주문을 수신하게 작성되어 있다.

아래와 같이 실행한 결과이다.

![image](https://user-images.githubusercontent.com/44644430/119429797-8fa33700-bd4a-11eb-94d9-0c79e9954471.png)

위와 같이 Order 서비스에서 주문을 생성하게 될 경우 아래와 같이 Payment 서비스에서 payment를 paid 상태로 업데이트 하게 된다. 

![image](https://user-images.githubusercontent.com/44644430/119429922-c8431080-bd4a-11eb-9f80-d48e11a7106f.png)

위와 같이 Payment 서비스에서 paid 상태로 업데이트 하면서 이벤트를 발신하게 되고 이를 수신 받은 Ordermanagement 서비스에서 ordermanagement를 아래와 같이 수신 및 저장하게 된다.

![image](https://user-images.githubusercontent.com/44644430/119430009-f1fc3780-bd4a-11eb-916b-de2f86ee2d49.png)

![image](https://user-images.githubusercontent.com/44644430/119430043-04767100-bd4b-11eb-8db9-a3e31d4bd04a.png)


**SAGA 패턴에 맞춘 SAGA Roll-Back 구성**

![image](https://user-images.githubusercontent.com/44644430/119428313-97ada780-bd47-11eb-9ea6-cfeb764de2b6.png)

위와 같이 현재 FlowerDelivery 시스템에서는 Choreograpy 방식으로 SAGA 패턴이 구현되도록 설계되어 있다.
아래 예시는 OrdermMnagement 서비스에서 OrderReject가 발생했을때 이다.
위 설계를 통해서 예상되는 결과물은 OrderManagement서비스에서도 삭제가 이루어지고 발행된 이벤트가 Payment 서비스에서 해당 order의 Payment도 삭제를 하면서
보상 이벤트를 발행하는것이다.

아래가 실행을 통한 결과이다.

![image](https://user-images.githubusercontent.com/44644430/119435346-e6157300-bd54-11eb-91b1-9056cb0028f5.png)

위와 같이  OrderReject로 OrderManagement 서비스에서 삭제가 이루어 질 경우 이벤트를 발생시켜 Payments 쪽에서도 삭제가 발생하게 된다.
위 두번째 커맨드를 통해서 payment에서도 삭제가 된것을 확인 할 수 있다.
아래 처럼 OrderManagement 서비스에서 OrderReject를 통해서 발생한 이벤트가 Payment 서비스의 ForciblyCanceled 이벤트를 발생시키는 것을 볼 수 있다.

![image](https://user-images.githubusercontent.com/44644430/119435389-f9284300-bd54-11eb-902a-87c7abfee9e3.png)


SAGA 패턴 - 이기정 ver

신규 추가한 상품(아이템)서비스에는 재고 수량을 관리한다.

주문 -> 결제 -> 상품 재고 차감 Flow 이며
재고가 부족할 경우 주문 -> 결제 -> 상품 재고차감 !!!!재고 부족 시 재고차감되지 않음 -> 주문취소 이벤트 -> 결제취소 이벤트 -> 재고 차감분 복구 ( 재고차감이벤트로 발생한 경우는 복구 하지 않음 )

위 Flow로 SAGA 패턴을 구현하였다.

장미세트 상품 5개 추가

```
C:\workspace\flowerdelivery>http POST http://localhost:8085/items itemName="roses set n1"   storeName=KJSHOP stockCnt=5 itemPrice=50000
HTTP/1.1 201
Content-Type: application/json;charset=UTF-8
Date: Wed, 09 Jun 2021 04:49:35 GMT
Location: http://localhost:8085/items/1     
Transfer-Encoding: chunked

{
    "_links": {
        "item": {
            "href": "http://localhost:8085/items/1"
        },
        "self": {
            "href": "http://localhost:8085/items/1"
        }
    },
    "itemName": "roses set n1",
    "itemPrice": 50000,
    "stockCnt": 5,
    "storeName": "KJSHOP"
}
```
LEEKIJUNG 이 3개 시켜서 재고 2개 남음

```
C:\workspace\flowerdelivery>http POST http://localhost:8081/orders storeName=KJSHOP itemId=1 itemName="roses set n1" qty=3 itemPrice=50000 userName=LEEKIJUNG
HTTP/1.1 201 
Content-Type: application/json;charset=UTF-8
Date: Wed, 09 Jun 2021 04:53:12 GMT
Location: http://localhost:8081/orders/8
Transfer-Encoding: chunked

{
    "_links": {
        "order": {
            "href": "http://localhost:8081/orders/8"
        },
        "self": {
            "href": "http://localhost:8081/orders/8"
        }
    },
    "itemId": 1,
    "itemName": "roses set n1",
    "itemPrice": 50000,
    "orderStatus": null,
    "qty": 3,
    "storeName": "KJSHOP",
    "userName": "LEEKIJUNG"
}

C:\workspace\flowerdelivery>http GET http://localhost:8085/items
HTTP/1.1 200 
Content-Type: application/hal+json;charset=UTF-8
Date: Wed, 09 Jun 2021 04:53:21 GMT
Transfer-Encoding: chunked

{
    "_embedded": {
        "items": [
            {
                "_links": {
                    "item": {
                        "href": "http://localhost:8085/items/1"
                    },
                    "self": {
                        "href": "http://localhost:8085/items/1"
                    }
                },
                "itemName": "roses set n1",
                "itemPrice": 50000,
                "stockCnt": 2,
                "storeName": "KJSHOP"
            }
        ]
    },
    "_links": {
        "profile": {
            "href": "http://localhost:8085/profile/items"
        },
        "self": {
            "href": "http://localhost:8085/items{?page,size,sort}",
            "templated": true
        }
    },
    "page": {
        "number": 0,
        "size": 20,
        "totalElements": 1,
        "totalPages": 1
    }
}
```

HONG 이 3개 추가 주문함

```
C:\workspace\flowerdelivery>http POST http://localhost:8081/orders storeName=KJSHOP itemId=1 itemName="roses set n1" qty=3 itemPrice=50000 userName=HONG
HTTP/1.1 201 
Content-Type: application/json;charset=UTF-8
Date: Wed, 09 Jun 2021 04:53:49 GMT
Location: http://localhost:8081/orders/10
Transfer-Encoding: chunked

{
    "_links": {
        "order": {
            "href": "http://localhost:8081/orders/10"
        },
        "self": {
            "href": "http://localhost:8081/orders/10"
        }
    },
    "itemId": 1,
    "itemName": "roses set n1",
    "itemPrice": 50000,
    "orderStatus": null,
    "qty": 3,
    "storeName": "KJSHOP",
    "userName": "HONG"
}
```

상품 재고는 그대로 2개 이며 

```
C:\workspace\flowerdelivery>http GET http://localhost:8085/items
HTTP/1.1 200 
Content-Type: application/hal+json;charset=UTF-8
Date: Wed, 09 Jun 2021 04:54:13 GMT
Transfer-Encoding: chunked

{
    "_embedded": {
        "items": [
            {
                "_links": {
                    "item": {
                        "href": "http://localhost:8085/items/1"
                    },
                    "self": {
                        "href": "http://localhost:8085/items/1"
                    }
                },
                "itemName": "roses set n1",
                "itemPrice": 50000,
                "stockCnt": 2,
                "storeName": "KJSHOP"
            }
        ]
    },
    "_links": {
        "profile": {
            "href": "http://localhost:8085/profile/items"
        },
        "self": {
            "href": "http://localhost:8085/items{?page,size,sort}",
            "templated": true
        }
    },
    "page": {
        "number": 0,
        "size": 20,
        "totalElements": 1,
        "totalPages": 1
    }
}
```
2번쨰 주문는 주문취소상태 2번째 결제는 결제취소상태로 각각 변경됨 

```
C:\workspace\flowerdelivery>http GET http://localhost:8081/orders
HTTP/1.1 200 
Content-Type: application/hal+json;charset=UTF-8
Date: Wed, 09 Jun 2021 04:54:35 GMT
Transfer-Encoding: chunked

{
    "_embedded": {
        "orders": [
            {
                "_links": {
                    "order": {
                        "href": "http://localhost:8081/orders/8"
                    },
                    "self": {
                        "href": "http://localhost:8081/orders/8"
                    }
                },
                "itemId": 1,
                "itemName": "roses set n1",
                "itemPrice": 50000,
                "orderStatus": null,
                "qty": 3,
                "storeName": "KJSHOP",
                "userName": "LEEKIJUNG"
            },
            {
                "_links": {
                    "order": {
                        "href": "http://localhost:8081/orders/10"
                    },
                    "self": {
                        "href": "http://localhost:8081/orders/10"
                    }
                },
                "itemId": 1,
                "itemName": "roses set n1",
                "itemPrice": 50000,
                "orderStatus": "OrderCancelled",
                "qty": 3,
                "storeName": "KJSHOP",
                "userName": "HONG"
            }
        ]
    },
    "_links": {
        "profile": {
            "href": "http://localhost:8081/profile/orders"
        },
        "self": {
            "href": "http://localhost:8081/orders{?page,size,sort}",
            "templated": true
        }
    },
    "page": {
        "number": 0,
        "size": 20,
        "totalElements": 2,
        "totalPages": 1
    }
}

C:\workspace\flowerdelivery>http GET http://localhost:8082/payments
HTTP/1.1 200 
Content-Type: application/hal+json;charset=UTF-8
Date: Wed, 09 Jun 2021 04:54:21 GMT
Transfer-Encoding: chunked

{
    "_embedded": {
        "payments": [
            {
                "_links": {
                    "payment": {
                        "href": "http://localhost:8082/payments/1"
                    },
                    "self": {
                        "href": "http://localhost:8082/payments/1"
                    }
                },
                "itemId": 1,
                "itemName": "roses set n1",
                "itemPrice": 50000,
                "orderId": 8,
                "paymentStatus": "paid",
                "qty": 3,
                "storeName": "KJSHOP",
                "userName": null
            },
            {
                "_links": {
                    "payment": {
                        "href": "http://localhost:8082/payments/2"
                    },
                    "self": {
                        "href": "http://localhost:8082/payments/2"
                    }
                },
                "itemId": 1,
                "itemName": "roses set n1",
                "itemPrice": 50000,
                "orderId": 10,
                "paymentStatus": "paid",
                "qty": 3,
                "storeName": "KJSHOP",
                "userName": null
            },
            {
                "_links": {
                    "payment": {
                        "href": "http://localhost:8082/payments/3"
                    },
                    "self": {
                        "href": "http://localhost:8082/payments/3"
                    }
                },
                "itemId": null,
                "itemName": null,
                "itemPrice": null,
                "orderId": 10,
                "paymentStatus": "paymentCanceled",
                "qty": null,
                "storeName": null,
                "userName": null
            }
        ]
    },
    "_links": {
        "profile": {
            "href": "http://localhost:8082/profile/payments"
        },
        "self": {
            "href": "http://localhost:8082/payments{?page,size,sort}",
            "templated": true
        }
    },
    "page": {
        "number": 0,
        "size": 20,
        "totalElements": 3,
        "totalPages": 1
    }
}
```



# 운영

## CICD설정

AWS Codebuild 를 활용하여 CI/CD 를 설정하였다. 

CODEBUILD 

소스 설정 

![image](https://user-images.githubusercontent.com/80744199/119442382-b4a3a400-bd62-11eb-9040-debd81ce3f85.png)



환경설정 ( 환경변수  계정, KUBE_URL, TOKEN 추가  ) 

![image](https://user-images.githubusercontent.com/80744199/119442432-c9803780-bd62-11eb-80be-28d96b7b8919.png)



빌드스펙

Buildspec.yml는 flowerdelivery git의 각 하윅  order / payment / ordermanagement ....  프로젝트 내에 각기 들어가 있으며
동일한 git 리포지토리를 활용하기 위에 아래와 같이  각 하위 프로젝트의 buildspec을 호출한다. 

![image](https://user-images.githubusercontent.com/80744199/119442808-76f34b00-bd63-11eb-95c4-60cb448afe94.png)


Buildspec.yml 내용 
```
version: 0.2

env:
  variables:
    _PROJECT_NAME: "user03-ordermanagement"

phases:
  install:
    runtime-versions:
      java: openjdk8
      docker: 18
    commands:
      - echo install kubectl
      - curl -LO https://storage.googleapis.com/kubernetes-release/release/$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)/bin/linux/amd64/kubectl
      - chmod +x ./kubectl
      - mv ./kubectl /usr/local/bin/kubectl
  pre_build:
    commands:
      - echo Logging in to Amazon ECR...
      - echo $_PROJECT_NAME
      - echo $AWS_ACCOUNT_ID
      - echo $AWS_DEFAULT_REGION
      - echo $CODEBUILD_RESOLVED_SOURCE_VERSION
      - echo start command
      - $(aws ecr get-login --no-include-email --region $AWS_DEFAULT_REGION)
      - cd ordermanagement
      - echo $PWD
  build:
    commands:
      - echo Build started on `date`
      - echo Building the Docker image...
      - mvn package -Dmaven.test.skip=true
      - docker build -t $AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com/$_PROJECT_NAME:$CODEBUILD_RESOLVED_SOURCE_VERSION  .
  post_build:
    commands:
      - echo Pushing the Docker image...
      - docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com/$_PROJECT_NAME:$CODEBUILD_RESOLVED_SOURCE_VERSION
      - echo connect kubectl
      - kubectl config set-cluster k8s --server="$KUBE_URL" --insecure-skip-tls-verify=true
      - kubectl config set-credentials admin --token="$KUBE_TOKEN"
      - kubectl config set-context default --cluster=k8s --user=admin
      - kubectl config use-context default
      - |
          cat <<EOF | kubectl apply -f -
          apiVersion: v1
          kind: Service
          metadata:
            name: $_PROJECT_NAME
            namespace: flowerdelivery
            labels:
              app: $_PROJECT_NAME
          spec:
            ports:
              - port: 8080
                targetPort: 8080
            selector:
              app: $_PROJECT_NAME
          EOF
      - |
          cat  <<EOF | kubectl apply -f -
          apiVersion: apps/v1
          kind: Deployment
          metadata:
            name: $_PROJECT_NAME
            namespace: flowerdelivery
            labels:
              app: $_PROJECT_NAME
          spec:
            replicas: 1
            selector:
              matchLabels:
                app: $_PROJECT_NAME
            template:
              metadata:
                labels:
                  app: $_PROJECT_NAME
              spec:
                containers:
                  - name: $_PROJECT_NAME
                    image: $AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com/$_PROJECT_NAME:$CODEBUILD_RESOLVED_SOURCE_VERSION
                    ports:
                      - containerPort: 8080
                    readinessProbe:
                      httpGet:
                        path: '/ordermanagements'
                        port: 8080
                      initialDelaySeconds: 10
                      timeoutSeconds: 2
                      periodSeconds: 5
                      failureThreshold: 10
                    livenessProbe:
                      httpGet:
                        path: '/ordermanagements'
                        port: 8080
                      initialDelaySeconds: 120
                      timeoutSeconds: 2
                      periodSeconds: 5
                      failureThreshold: 5
          EOF
cache:
  paths:
    - '/root/.m2/**/*'
```


빌드가 성공한 모습

![image](https://user-images.githubusercontent.com/80744199/119442348-a2296a80-bd62-11eb-8b45-3d3ef9da96a2.png)


개인 클라우드에 재구축함 

![image](https://user-images.githubusercontent.com/80744199/121292470-f7e64100-c924-11eb-9147-fccda792ff8e.png)



## 동기식 호출 서킷 브레이킹 장애격리

서킷 브레이킹 프레임워크의 선택: Spring FeignClient + Hystrix 옵션을 사용하여 구현함
주문 - 결제간 신규 주문시 결제처리를 RestFul Req/Res 로 구현하였으며, 결제 요청이 과도할 경우 서킷 브레이크를 통해 장애 격리를 하려고 한다.

Hystrix 를 설정: 요청처리 쓰레드에서 처리시간이 610 밀리가 넘어서기 시작하여 어느정도 유지되면 CB 회로가 닫히도록 (요청을 빠르게 실패처리, 차단) 설정

![image](https://user-images.githubusercontent.com/80744199/121296950-785c7000-c92c-11eb-9e50-3b561bdcefd3.png)

결제 서비스의 부하 처리 - 400 밀리에서 증감 220 밀리 정도 왔다갔다 하게

![image](https://user-images.githubusercontent.com/80744199/121297163-da1cda00-c92c-11eb-9529-cc28dd361489.png)

부하테스터 siege 툴을 통한 서킷 브레이커 동작 확인:
동시사용자 100명
60초 동안 실시

```
C:\workspace\flowerdelivery>kubectl exec -it --namespace istio-cb-ns siege bin/bash
kubectl exec [POD] [COMMAND] is DEPRECATED and will be removed in a future version. Use kubectl exec [POD] -- [COMMAND] instead.
root@siege:/# siege -c100 -t60S -r10 -v --content-type "application/json" 'http://a64bd0a2780534decae2fcf1f45cdc96-2126150052.ap-northeast-2.elb.amazonaws.com:8080/orders POST {"storeName": "flowershop", "itemName": "rose", "qty": "1", "itemPrice": "20000", "userName": "LEE", "itemId": "1"}'
```

요청 상태에 따라 회로 열기/닫기가 반복되는 모습

![image](https://user-images.githubusercontent.com/80744199/121304446-4ef51180-c937-11eb-9f76-f7d33c772e85.png)

![image](https://user-images.githubusercontent.com/80744199/121304595-85329100-c937-11eb-909d-1fbf1f4dcd72.png)

고객 사용성이 좋지 않기 때문에 오토스케일 아웃 등의 설정을 통해 후속 처리가 필요함


### 오토스케일 아웃

payment 서비스에 HPA를 설정한다.  평균대비 CPU 50퍼 초과시 6개까지 pod 추가 

```
C:\workspace\flowerdelivery>kubectl autoscale deployment.apps/payment --cpu-percent=50 --min=1 --max=6
horizontalpodautoscaler.autoscaling/payment autoscaled

C:\workspace\flowerdelivery>kubectl get all
NAME                           READY   STATUS    RESTARTS   AGE
pod/gateway-6f67fb9bf9-t2zc5   1/1     Running   0          26m
pod/order-96bb9df98-spgvl      1/1     Running   0          9m36s
pod/payment-7c657f9b-vkvzr     1/1     Running   0          28m

NAME                 TYPE           CLUSTER-IP       EXTERNAL-IP                                                                   PORT(S)          AGE
service/gateway      LoadBalancer   10.100.17.208    a1f4f458259eb4e4abd9bd67ef8211db-641677351.ap-northeast-2.elb.amazonaws.com   8080:30760/TCP   26m
service/kubernetes   ClusterIP      10.100.0.1       <none>                                                                        443/TCP          8h
service/order        ClusterIP      10.100.108.3     <none>                                                                        8080/TCP         9m36s
service/payment      ClusterIP      10.100.241.200   <none>                                                                        8080/TCP         28m

NAME                      READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/gateway   1/1     1            1           26m
deployment.apps/order     1/1     1            1           9m36s
deployment.apps/payment   1/1     1            1           28m

NAME                                 DESIRED   CURRENT   READY   AGE
replicaset.apps/gateway-6f67fb9bf9   1         1         1       26m
replicaset.apps/order-96bb9df98      1         1         1       9m36s
replicaset.apps/payment-7c657f9b     1         1         1       28m

NAME                                          REFERENCE            TARGETS   MINPODS   MAXPODS   REPLICAS   AGE
horizontalpodautoscaler.autoscaling/payment   Deployment/payment   6%/50%    1         6         1          17s

```
payment 서비스에 대한 모니터링을 걸어두고  siege 로 부하테스트를 진행하면 

![image](https://user-images.githubusercontent.com/80744199/121320401-17db2c00-c948-11eb-8b44-39891a3b14f5.png)


아래와 같이 scale out 되는것을 확인할 수 있다. 

![image](https://user-images.githubusercontent.com/80744199/121320618-4953f780-c948-11eb-87ea-7651c9385e5d.png)



pod 자원을 모니터링 

![image](https://user-images.githubusercontent.com/80744199/121320485-2f1a1980-c948-11eb-9a43-b13335d4de64.png)


### 무정지배포

신규로 추가한 상품서비스의 배포 스크립트에 readinessProbe 옵션을 추가한다. 
```
apiVersion: v1
kind: Service
metadata:
  name: item
  labels:
    app: item
spec:
  ports:
    - port: 8080
      targetPort: 8080
  selector:
    app: item
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: item
  labels:
    app: item
spec:
  selector:
    matchLabels:
      app: item
  replicas: 2
  template:
    metadata:
      labels:
        app: item
    spec:
      containers:
      - name: item
        image: 583098675101.dkr.ecr.ap-northeast-2.amazonaws.com/item:v1
        ports:
        - containerPort: 8080
        readinessProbe:
          httpGet:
            path: '/items'
            port: 8080
          initialDelaySeconds: 10
          timeoutSeconds: 2
          periodSeconds: 5
          failureThreshold: 10        
---
```

```
C:\workspace\flowerdelivery\item>kubectl apply -f kube-item-ready.yml
service/item created
deployment.apps/item created
```
배포된 모습 
![image](https://user-images.githubusercontent.com/80744199/121331140-9092b600-c951-11eb-8e95-220a467a265f.png)

siege 에 상품 서비스를 계속 호출 하게 설정 한후 
```
siege -c100 -t120S -v --content-type "application/json" 'http://10.100.6.190:8080/items POST {"storeName": "KJSHOP", "itemName": "roses set n1", "stockCnt": "5", "itemPrice": "20000"}'

```

Set image 명령어를 통해 배포를 수행 한다. 

![image](https://user-images.githubusercontent.com/10009227/121339077-149c6c00-c959-11eb-80ff-ec6f37674d60.png)


siege를 통해 100% 가용성을 확인했으므로  무정지 배포가 되었다. 

![image](https://user-images.githubusercontent.com/80744199/121331228-a43e1c80-c951-11eb-83b9-faab9c564c25.png)





## ConfigMap

ConfigMaps는 컨테이너 이미지로부터 설정 정보를 분리할 수 있도록 Kubernetes에서 제공해주는 설정이다. 환경변수나 설정값 들을 환경변수로 관리해 Pod가 생성될 때 이 값을 주입할 수 있다.
Flowerdelivery 시스템에서는 namespace 값을 저장하여 사용하기 위해서 아래와 같이 flowerdelivery-config라는 이름의 config map 에 flowerdelivery라는 변수로 namespace의 값을 저장했다.

flowerdelivery-config.yml

![image](https://user-images.githubusercontent.com/80744199/121285031-acc63100-c918-11eb-81a3-84181ca9cd10.png)

```
C:\workspace\flowerdelivery>kubectl apply -f flowerdelivery-config.yml
configmap/flowerdelivery-config created

```

buildspec.yml 에 아래와 같이 namespacename 이라는 환경 변수에 위 컨피그 맵에서 정의한 nsname의 값을 설정한다.

![image](https://user-images.githubusercontent.com/80744199/121285217-f44cbd00-c918-11eb-87be-a6c8554f01a1.png)

빌드/배포 후에 

![image](https://user-images.githubusercontent.com/80744199/121285340-31b14a80-c919-11eb-8478-e5e33ed1a84d.png)


POD 로 진입하여 환경변수 및 Echo로 namespacename 값을 확인한다.

pod진입

```
C:\workspace\flowerdelivery>kubectl exec -it -n flowerdelivery pod/order-58dd6cf76f-4wsrs /bin/bash
kubectl exec [POD] [COMMAND] is DEPRECATED and will be removed in a future version. Use kubectl exec [POD] -- [COMMAND] instead.
root@order-58dd6cf76f-4wsrs:/# 
root@order-58dd6cf76f-4wsrs:/# 
root@order-58dd6cf76f-4wsrs:/# env
```

env 확인

![image](https://user-images.githubusercontent.com/80744199/121285490-71783200-c919-11eb-923b-a2267ccd00e6.png)

echo 로 환경변수 확인

```
root@order-58dd6cf76f-4wsrs:/# echo $namespacename
flowerdelivery
```

## livenessProbe 

주문관리 서비스 (ordermanagement)의 Pod 생성 스크립트에 livenessProbe 구문을 추가한다. 

Pod 내 /tmp/healthy 파일을 체크함

```
apiVersion: v1
kind: Pod
metadata:
  name: ordermanagement
  labels:
    app: ordermanagement
spec:
  containers:
  - name: ordermanagement
    image: 583098675101.dkr.ecr.ap-northeast-2.amazonaws.com/ordermanagement:latest
    livenessProbe:
      exec:
        command:
        - cat 
        - /tmp/healthy
      initialDelaySeconds: 5
      periodSeconds: 5
```

pod 생성 
```
C:\workspace\flowerdelivery\ordermanagement>kubectl create -f kube-liveness.yml
pod/ordermanagement created
```


Pod를 구동하면 running 으로 나오지만 
/tmp/healthy 파일이 없기 때문에 계속 restart 된다. "셀프 힐링"

![image](https://user-images.githubusercontent.com/10009227/121382800-ea12d900-c981-11eb-91ee-e7d0854590f3.png)


pod describe 로 확인하면  livenessProbe 실패 로그가 확인됨 
![image](https://user-images.githubusercontent.com/10009227/121382922-06167a80-c982-11eb-8d08-733175acd096.png)


ordermanagement Pod로 진입하여  /tmp/healthy 를 생성하면 restart가 중단되고, Pod가 정상동작한다. 

```
C:\workspace\flowerdelivery>kubectl exec -it pod/ordermanagement /bin/bash
kubectl exec [POD] [COMMAND] is DEPRECATED and will be removed in a future version. Use kubectl exec [POD] -- [COMMAND] instead.

root@ordermanagement:/tmp# touch /tmp/healthy
root@ordermanagement:/tmp# ls -al
total 0
drwxrwxrwt 1 root root 196 Jun  9 15:17 .
drwxr-xr-x 1 root root  40 Jun  9 15:17 ..
-rw-r--r-- 1 root root   0 Jun  9 15:17 healthy
drwxr-xr-x 1 root root  15 Jun  9 15:17 hsperfdata_root
-rw-r--r-- 1 root root   0 Jun  9 15:17 kafka-client-jaas-config-placeholder251220694542028385conf
drwxr-xr-x 2 root root   6 Jun  9 15:17 tomcat-docbase.5300142031336227160.8080
drwxr-xr-x 3 root root  18 Jun  9 15:17 tomcat.5226006221561639455.8080
```

3회 실패 후  healthy 파일 생성되어 4번째에 성공 
Liveness:       exec [cat /tmp/healthy] delay=5s timeout=1s period=5s #success=1 #failure=3  
