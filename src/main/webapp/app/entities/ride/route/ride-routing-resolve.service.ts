import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { EMPTY, Observable, of } from 'rxjs';
import { mergeMap } from 'rxjs/operators';

import { IRide } from '../ride.model';
import { RideService } from '../service/ride.service';

const rideResolve = (route: ActivatedRouteSnapshot): Observable<null | IRide> => {
  const id = route.params.id;
  if (id) {
    return inject(RideService)
      .find(id)
      .pipe(
        mergeMap((ride: HttpResponse<IRide>) => {
          if (ride.body) {
            return of(ride.body);
          }
          inject(Router).navigate(['404']);
          return EMPTY;
        }),
      );
  }
  return of(null);
};

export default rideResolve;
