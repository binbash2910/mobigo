import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { EMPTY, Observable, of } from 'rxjs';
import { mergeMap } from 'rxjs/operators';

import { IGroupMember } from '../group-member.model';
import { GroupMemberService } from '../service/group-member.service';

const groupMemberResolve = (route: ActivatedRouteSnapshot): Observable<null | IGroupMember> => {
  const id = route.params.id;
  if (id) {
    return inject(GroupMemberService)
      .find(id)
      .pipe(
        mergeMap((groupMember: HttpResponse<IGroupMember>) => {
          if (groupMember.body) {
            return of(groupMember.body);
          }
          inject(Router).navigate(['404']);
          return EMPTY;
        }),
      );
  }
  return of(null);
};

export default groupMemberResolve;
