# Bài 2 — Tách dịch vụ theo phong cách SOA với hợp đồng dịch vụ rõ ràng

## 1. Mục tiêu

LibraX bổ sung `NotificationService` để gửi thông báo cho độc giả khi sách đã quá hạn trả.

`BorrowingService` gửi message thông qua tầng trung gian `EsbSimulator`.

```text
BorrowingService
       |
       | Message
       v
EsbSimulator (ESB)
       |
       | Route
       v
NotificationService
```

---

## 2. Phân tích lỗi ESB Simulator

### 2.1. Lỗi sử dụng `==`

Code ban đầu:

```java
if (toService == "NotificationService") {
    notificationService.handle(operation, payload);
}
```

Trong Java, `==` với String kiểm tra reference của object, không phải nội dung chuỗi.

Ví dụ:

```java
String service = new String("NotificationService");

System.out.println(service == "NotificationService");
// false

System.out.println(service.equals("NotificationService"));
// true
```

Cách đúng:

```java
if ("NotificationService".equals(toService)) {
    notificationService.handle(operation, payload);
}
```

Việc đặt chuỗi cố định ở phía trước cũng giúp tránh `NullPointerException` khi `toService` là `null`.

### 2.2. Routing thất bại

Code ban đầu không có nhánh xử lý khi `toService` không tồn tại.

Code mới phải ghi log cảnh báo:

```java
else {
    log.warn(
        "Unable to route message. Unknown service: {}",
        toService
    );
}
```

---

# 3. Service Contract

## Service Name

```text
NotificationService
```

## Operation

```text
notifyOverdue(memberId, bookId, dueDate)
```

## Mục đích

Gửi thông báo đến độc giả khi một cuốn sách đã quá hạn trả.

## Input

| Field | Type | Required | Description |
|---|---|---|---|
| memberId | Long | Yes | ID của độc giả |
| bookId | Long | Yes | ID của sách |
| dueDate | Date | Yes | Ngày đến hạn trả sách |

## Output

```text
NotificationResponse
```

| Field | Type | Description |
|---|---|---|
| success | Boolean | Kết quả xử lý |
| message | String | Thông tin kết quả |

## Error

```text
MEMBER_NOT_FOUND
BOOK_NOT_FOUND
INVALID_DUE_DATE
NOTIFICATION_FAILED
```

---

# 4. WSDL rút gọn

```xml
<definitions
    name="NotificationService"
    targetNamespace="http://librax.com/notification">

    <types>

        <element name="notifyOverdueRequest">
            <complexType>
                <sequence>
                    <element
                        name="memberId"
                        type="long"/>

                    <element
                        name="bookId"
                        type="long"/>

                    <element
                        name="dueDate"
                        type="date"/>
                </sequence>
            </complexType>
        </element>

        <element name="notifyOverdueResponse">
            <complexType>
                <sequence>
                    <element
                        name="success"
                        type="boolean"/>

                    <element
                        name="message"
                        type="string"/>
                </sequence>
            </complexType>
        </element>

    </types>

    <message name="NotifyOverdueRequest">
        <part
            name="parameters"
            element="tns:notifyOverdueRequest"/>
    </message>

    <message name="NotifyOverdueResponse">
        <part
            name="parameters"
            element="tns:notifyOverdueResponse"/>
    </message>

    <portType name="NotificationServicePortType">

        <operation name="notifyOverdue">

            <input message="tns:NotifyOverdueRequest"/>

            <output message="tns:NotifyOverdueResponse"/>

        </operation>

    </portType>

    <binding
        name="NotificationServiceBinding"
        type="tns:NotificationServicePortType">

        <operation name="notifyOverdue">
            <soap:operation
                soapAction="notifyOverdue"/>
        </operation>

    </binding>

    <service name="NotificationService">

        <port
            name="NotificationServicePort"
            binding="tns:NotificationServiceBinding">

            <soap:address
                location="http://localhost:8081/notification"/>

        </port>

    </service>

</definitions>
```

---

# 5. Message mẫu

Khi `BorrowingService` phát hiện sách quá hạn:

```json
{
    "operation": "notifyOverdue",
    "memberId": 1001,
    "bookId": 2001,
    "dueDate": "2026-09-01"
}
```

Thông tin routing:

```text
toService = NotificationService
operation = notifyOverdue
payload = JSON message
```

---

# 6. Luồng xử lý

### Bước 1 — BorrowingService phát hiện quá hạn

`BorrowingService` kiểm tra:

```text
Current Date > Due Date
```

Nếu sách quá hạn, service tạo message chứa:

```text
memberId
bookId
dueDate
```

### Bước 2 — Gửi message tới ESB

```text
routeMessage(
    "NotificationService",
    "notifyOverdue",
    payload
)
```

### Bước 3 — ESB xác định service đích

ESB kiểm tra:

```java
"NotificationService".equals(toService)
```

Nếu đúng, ESB chuyển message đến `NotificationService`.

### Bước 4 — NotificationService xử lý

Service nhận:

```text
operation = notifyOverdue
payload = {
    memberId,
    bookId,
    dueDate
}
```

Sau đó thực hiện nghiệp vụ gửi thông báo.

### Bước 5 — Trả kết quả

```json
{
    "success": true,
    "message": "Overdue notification sent successfully"
}
```

---

# 7. Tổng quan luồng

```text
┌─────────────────────┐
│  BorrowingService   │
└──────────┬──────────┘
           │
           │ Detect overdue
           v
┌─────────────────────┐
│ Create Message      │
│ notifyOverdue       │
└──────────┬──────────┘
           │
           │ routeMessage()
           v
┌─────────────────────┐
│    ESB Simulator    │
│                     │
│ "NotificationService│
│      ".equals()     │
└──────────┬──────────┘
           │
           │ Route
           v
┌─────────────────────┐
│ NotificationService │
└──────────┬──────────┘
           │
           │ Process
           v
┌─────────────────────┐
│ Send Notification   │
└─────────────────────┘
```

Nếu service không tồn tại:

```text
BorrowingService
       |
       v
ESB Simulator
       |
       v
UnknownService
       |
       v
log.warn(...)
```

---

# 8. Code ESB Simulator hoàn chỉnh

```java
package com.librax.esb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EsbSimulator {

    private static final Logger log =
            LoggerFactory.getLogger(EsbSimulator.class);

    private final NotificationService notificationService;
    private final PaymentService paymentService;

    public EsbSimulator(
            NotificationService notificationService,
            PaymentService paymentService
    ) {
        this.notificationService = notificationService;
        this.paymentService = paymentService;
    }

    public void routeMessage(
            String toService,
            String operation,
            String payload
    ) {

        if ("NotificationService".equals(toService)) {

            log.info(
                    "Routing message to NotificationService. Operation: {}",
                    operation
            );

            notificationService.handle(
                    operation,
                    payload
            );

        } else if ("PaymentService".equals(toService)) {

            log.info(
                    "Routing message to PaymentService. Operation: {}",
                    operation
            );

            paymentService.handle(
                    operation,
                    payload
            );

        } else {

            log.warn(
                    "Unable to route message. Unknown service: {}. Operation: {}",
                    toService,
                    operation
            );
        }
    }
}
```

---

# 9. Kết luận

Bài toán áp dụng tư tưởng SOA bằng cách tách các chức năng thành các service có contract rõ ràng.

- `BorrowingService`: phát hiện sách quá hạn.
- `EsbSimulator`: đóng vai trò ESB, định tuyến message.
- `NotificationService`: xử lý và gửi thông báo.
- Service Contract xác định operation `notifyOverdue` cùng các input `memberId`, `bookId`, `dueDate`.
- Lỗi `==` đã được thay bằng `.equals()`.
- Bổ sung logging khi routing thất bại.

Luồng tổng quát:

```text
BorrowingService
      ↓
   Message
      ↓
ESB Simulator
      ↓
NotificationService
      ↓
Process Notification
```
