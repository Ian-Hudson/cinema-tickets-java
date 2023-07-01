package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.paymentgateway.TicketPaymentServiceImpl;
import thirdparty.seatbooking.SeatReservationService;
import thirdparty.seatbooking.SeatReservationServiceImpl;
import uk.gov.dwp.uc.pairtest.domain.CustomerTicketOrder;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class TicketServiceImpl implements TicketService {
    /**
     * Should only have private methods other than the one below.
     */


    /**
     *
     * @param ticketTypeRequests
     * @throws InvalidPurchaseException
     *
     * Validate that the tickets requested contain at least one adult ticket or throw an exception
     * Also prevents the purchase of child and infant tickets in the event of zero adult tickets requested
     *
     * Uses Supplier<Stream<TicketTypeRequest>> to prevent the exception 'Stream has already been operated upon or closed'
     */
    private void validateTicketTypeRequest(TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        Supplier<Stream<TicketTypeRequest>> supplier
                = () -> Arrays.stream(ticketTypeRequests);


        boolean hasAdult = supplier.get().anyMatch(ticketTypeRequest
                -> ticketTypeRequest.getTicketType() == TicketTypeRequest.Type.ADULT);


        if(!hasAdult)
            throw new InvalidPurchaseException("TicketTypeRequest does not contain minimum 1 adult ticket type");

        int totalTickets = supplier.get().mapToInt( ticketTypeRequest-> ticketTypeRequest.getNoOfTickets()).sum();



        if(totalTickets > 20)
            throw new InvalidPurchaseException("TicketTypeRequest ticket count exceeds max allocation of 20");

    }

    /**
     *
     * @param accountId
     * @param ticketTypeRequests
     * @return CustomerTicketOrder
     * @throws InvalidPurchaseException
     *
     * Again using Supplier<Stream<TicketTypeRequest>> and applying filters to the stream processing to
     * generate the order counts.
     */
    private CustomerTicketOrder generateCustomerTicketOrder(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException{

        Supplier<Stream<TicketTypeRequest>> supplier
                = () -> Arrays.stream(ticketTypeRequests);

        int adultCount = supplier.get().filter(ticketTypeRequest -> ticketTypeRequest.getTicketType() == TicketTypeRequest.Type.ADULT)
                .mapToInt( ticketTypeRequest-> ticketTypeRequest.getNoOfTickets())
                .sum();

        if(adultCount < 1)
            new InvalidPurchaseException("TicketTypeRequest does not contain minimum 1 adult ticket type");



        int childCount = supplier.get().filter(ticketTypeRequest -> ticketTypeRequest.getTicketType() == TicketTypeRequest.Type.CHILD)
                .mapToInt( ticketTypeRequest-> ticketTypeRequest.getNoOfTickets())
                .sum();



        int infantCount = supplier.get().filter(ticketTypeRequest -> ticketTypeRequest.getTicketType() == TicketTypeRequest.Type.INFANT)
                .mapToInt( ticketTypeRequest-> ticketTypeRequest.getNoOfTickets())
                .sum();



        CustomerTicketOrder customerTicketOrderRecord = new CustomerTicketOrder(accountId, adultCount, childCount, infantCount);
        return customerTicketOrderRecord;
    }

    private int paymentTotal(CustomerTicketOrder customerTicketOrder){
        return (customerTicketOrder.adultCount() * 20) + (customerTicketOrder.infantCount() * 10) + (customerTicketOrder.infantCount() * 0);
    }

    private int seatTotal(CustomerTicketOrder customerTicketOrder){
        return (customerTicketOrder.adultCount()) + (customerTicketOrder.childCount());
    }

    /**
     *
     * @param accountId
     * @param ticketTypeRequests
     * @throws InvalidPurchaseException
     *
     * First reject any TicketTypeRequests that have an accountId of 0l
     * 1. Validate - Throw exceptions if fails
     * 2. Create a CustomerTicketOrder record
     * 3. Process payment via TicketPaymentService
     * 4. Reserve seats via SeatReservationService
     *
     * The Unit tests are testing for InvalidPurchaseExceptions being thrown to test the business rules.
     * Reflection could have been used to access private methods but having the purchaseTickets method expose more
     * to test would be simpler.
     */
    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        TicketPaymentService ticketPaymentService = new TicketPaymentServiceImpl();
        SeatReservationService seatReservationService = new SeatReservationServiceImpl();

        //Throw exception if accountId < 0
        if(accountId < 1l)
            throw new InvalidPurchaseException("0l is an invalid accountId in TicketTypeRequest");

        try {
            validateTicketTypeRequest(ticketTypeRequests);
            CustomerTicketOrder customerTicketOrderRecord = generateCustomerTicketOrder(accountId, ticketTypeRequests);

            //It would be better if the following two lines of code were wrapped in a single transaction to prevent
            //against seats still being reserved in the event that the payment services fails.

            ticketPaymentService.makePayment(customerTicketOrderRecord.accountId(), paymentTotal(customerTicketOrderRecord));
            seatReservationService.reserveSeat(customerTicketOrderRecord.accountId(),seatTotal(customerTicketOrderRecord) );

        }catch(InvalidPurchaseException e) {
            throw e;
        }
    }
}
