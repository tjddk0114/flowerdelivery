package flowerdelivery;

import flowerdelivery.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @StreamListener(KafkaProcessor.INPUT)
    public void onStringEventListener(@Payload String eventString){

    }

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPaid_AcceptRequest(@Payload Paid paid){

        if(paid.isMe()){
            System.out.println("##### listener AcceptRequest : " + paid.toJson());
            System.out.println();
            System.out.println();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPaymentCanceled_AcceptCancel(@Payload PaymentCanceled paymentCanceled){

        if(paymentCanceled.isMe()){
            System.out.println("##### listener AcceptCancel : " + paymentCanceled.toJson());
            System.out.println();
            System.out.println();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverDeliveryCompleted_UpdateOrdermanagementStatus(@Payload DeliveryCompleted deliveryCompleted){

        if(deliveryCompleted.isMe()){
            System.out.println("##### listener UpdateOrdermanagementStatus : " + deliveryCompleted.toJson());
            System.out.println();
            System.out.println();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverDepartedForDelivery_UpdateOrdermanagementStatus(@Payload DepartedForDelivery departedForDelivery){

        if(departedForDelivery.isMe()){
            System.out.println("##### listener UpdateOrdermanagementStatus : " + departedForDelivery.toJson());
            System.out.println();
            System.out.println();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverDeliveryCanceled_UpdateOrdermanagementStatus(@Payload DeliveryCanceled deliveryCanceled){

        if(deliveryCanceled.isMe()){
            System.out.println("##### listener UpdateOrdermanagementStatus : " + deliveryCanceled.toJson());
            System.out.println();
            System.out.println();
        }
    }

}