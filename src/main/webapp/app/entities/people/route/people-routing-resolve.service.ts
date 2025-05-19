import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { EMPTY, Observable, of } from 'rxjs';
import { mergeMap } from 'rxjs/operators';

import { IPeople } from '../people.model';
import { PeopleService } from '../service/people.service';

const peopleResolve = (route: ActivatedRouteSnapshot): Observable<null | IPeople> => {
  const id = route.params.id;
  if (id) {
    return inject(PeopleService)
      .find(id)
      .pipe(
        mergeMap((people: HttpResponse<IPeople>) => {
          if (people.body) {
            return of(people.body);
          }
          inject(Router).navigate(['404']);
          return EMPTY;
        }),
      );
  }
  return of(null);
};

export default peopleResolve;
