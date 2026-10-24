package ru.basted.ithelpdesk.core.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import ru.basted.ithelpdesk.ticket.entity.Ticket;

class ApiExceptionHandlerTest {
    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void badRequestIsReportedAsBadRequest() {
        var response = handler.handleBadRequest(new BadRequestException("Некорректный статус"));

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Некорректный статус", response.getBody().message());
    }

    @Test
    void optimisticLockConflictIsReportedAsConflict() {
        var response = handler.handleOptimisticLock(
                new ObjectOptimisticLockingFailureException(Ticket.class, 1L)
        );

        assertEquals(409, response.getStatusCode().value());
        assertEquals(
                "Заявка или запрос уже изменены другим пользователем. "
                        + "Обновите данные и повторите действие",
                response.getBody().message()
        );
    }
}
