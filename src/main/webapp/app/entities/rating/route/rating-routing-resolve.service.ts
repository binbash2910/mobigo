import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { EMPTY, Observable, of } from 'rxjs';
import { mergeMap } from 'rxjs/operators';

import { IRating } from '../rating.model';
import { RatingService } from '../service/rating.service';

const ratingResolve = (route: ActivatedRouteSnapshot): Observable<null | IRating> => {
  const id = route.params.id;
  if (id) {
    return inject(RatingService)
      .find(id)
      .pipe(
        mergeMap((rating: HttpResponse<IRating>) => {
          if (rating.body) {
            return of(rating.body);
          }
          inject(Router).navigate(['404']);
          return EMPTY;
        }),
      );
  }
  return of(null);
};

export default ratingResolve;
