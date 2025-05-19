import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { EMPTY, Observable, of } from 'rxjs';
import { mergeMap } from 'rxjs/operators';

import { IStep } from '../step.model';
import { StepService } from '../service/step.service';

const stepResolve = (route: ActivatedRouteSnapshot): Observable<null | IStep> => {
  const id = route.params.id;
  if (id) {
    return inject(StepService)
      .find(id)
      .pipe(
        mergeMap((step: HttpResponse<IStep>) => {
          if (step.body) {
            return of(step.body);
          }
          inject(Router).navigate(['404']);
          return EMPTY;
        }),
      );
  }
  return of(null);
};

export default stepResolve;
