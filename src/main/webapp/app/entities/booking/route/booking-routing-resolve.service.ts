import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { EMPTY, Observable, of } from 'rxjs';
import { mergeMap } from 'rxjs/operators';

import { IBooking } from '../booking.model';
import { BookingService } from '../service/booking.service';

const bookingResolve = (route: ActivatedRouteSnapshot): Observable<null | IBooking> => {
  const id = route.params.id;
  if (id) {
    return inject(BookingService)
      .find(id)
      .pipe(
        mergeMap((booking: HttpResponse<IBooking>) => {
          if (booking.body) {
            return of(booking.body);
          }
          inject(Router).navigate(['404']);
          return EMPTY;
        }),
      );
  }
  return of(null);
};

export default bookingResolve;
