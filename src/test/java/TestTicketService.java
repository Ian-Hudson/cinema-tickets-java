import org.junit.Test;
import uk.gov.dwp.uc.pairtest.TicketService;
import uk.gov.dwp.uc.pairtest.TicketServiceImpl;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class TestTicketService {

    @Test
    public void invalidAccountIdExceptionTest() {
        TicketTypeRequest[] ticketTypeArray = new TicketTypeRequest[1];
        ticketTypeArray[0] = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 5);

        TicketService ts = mock(TicketServiceImpl.class);

        try {
            doCallRealMethod().when(ts).purchaseTickets(anyLong(), any());
            ts.purchaseTickets(0l, ticketTypeArray);
            fail("Expected exception was not thrown");
        }catch(InvalidPurchaseException e ){
            verify(ts).purchaseTickets(0l, ticketTypeArray);
            assertEquals("0l is an invalid accountId in TicketTypeRequest", e.getMessage());
        }
    }

    @Test
    public void maximumTicketAllocationExceptionTest(){
        TicketTypeRequest[] ticketTypeArray = new TicketTypeRequest[1];
        ticketTypeArray[0] = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 21);

        TicketService ts = mock(TicketServiceImpl.class);

        try {
            doCallRealMethod().when(ts).purchaseTickets(anyLong(), any());
            ts.purchaseTickets(1l, ticketTypeArray);
            fail("Expected exception was not thrown");
        }catch(InvalidPurchaseException e ){
            verify(ts).purchaseTickets(1l, ticketTypeArray);
            assertEquals("TicketTypeRequest ticket count exceeds max allocation of 20", e.getMessage());
        }
    }

    @Test
    public void minimumOneAdultExceptionTest() {
        TicketTypeRequest[] ticketTypeArray = new TicketTypeRequest[1];
        ticketTypeArray[0] = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 10);

        TicketService ts = mock(TicketServiceImpl.class);

        try {
            doCallRealMethod().when(ts).purchaseTickets(anyLong(), any());
            ts.purchaseTickets(1l, ticketTypeArray);
            fail("Expected exception was not thrown");
        }catch(InvalidPurchaseException e ){
            verify(ts).purchaseTickets(1l, ticketTypeArray);
            assertEquals("TicketTypeRequest does not contain minimum 1 adult ticket type", e.getMessage());
        }

    }

}
