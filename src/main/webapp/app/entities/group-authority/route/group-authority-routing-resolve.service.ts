import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { EMPTY, Observable, of } from 'rxjs';
import { mergeMap } from 'rxjs/operators';

import { IGroupAuthority } from '../group-authority.model';
import { GroupAuthorityService } from '../service/group-authority.service';

const groupAuthorityResolve = (route: ActivatedRouteSnapshot): Observable<null | IGroupAuthority> => {
  const id = route.params.id;
  if (id) {
    return inject(GroupAuthorityService)
      .find(id)
      .pipe(
        mergeMap((groupAuthority: HttpResponse<IGroupAuthority>) => {
          if (groupAuthority.body) {
            return of(groupAuthority.body);
          }
          inject(Router).navigate(['404']);
          return EMPTY;
        }),
      );
  }
  return of(null);
};

export default groupAuthorityResolve;
