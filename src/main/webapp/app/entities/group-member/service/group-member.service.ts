import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable, asapScheduler, scheduled } from 'rxjs';

import { catchError } from 'rxjs/operators';

import { isPresent } from 'app/core/util/operators';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import { Search } from 'app/core/request/request.model';
import { IGroupMember, NewGroupMember } from '../group-member.model';

export type PartialUpdateGroupMember = Partial<IGroupMember> & Pick<IGroupMember, 'id'>;

export type EntityResponseType = HttpResponse<IGroupMember>;
export type EntityArrayResponseType = HttpResponse<IGroupMember[]>;

@Injectable({ providedIn: 'root' })
export class GroupMemberService {
  protected readonly http = inject(HttpClient);
  protected readonly applicationConfigService = inject(ApplicationConfigService);

  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/group-members');
  protected resourceSearchUrl = this.applicationConfigService.getEndpointFor('api/group-members/_search');

  create(groupMember: NewGroupMember): Observable<EntityResponseType> {
    return this.http.post<IGroupMember>(this.resourceUrl, groupMember, { observe: 'response' });
  }

  update(groupMember: IGroupMember): Observable<EntityResponseType> {
    return this.http.put<IGroupMember>(`${this.resourceUrl}/${this.getGroupMemberIdentifier(groupMember)}`, groupMember, {
      observe: 'response',
    });
  }

  partialUpdate(groupMember: PartialUpdateGroupMember): Observable<EntityResponseType> {
    return this.http.patch<IGroupMember>(`${this.resourceUrl}/${this.getGroupMemberIdentifier(groupMember)}`, groupMember, {
      observe: 'response',
    });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IGroupMember>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IGroupMember[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  search(req: Search): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<IGroupMember[]>(this.resourceSearchUrl, { params: options, observe: 'response' })
      .pipe(catchError(() => scheduled([new HttpResponse<IGroupMember[]>()], asapScheduler)));
  }

  getGroupMemberIdentifier(groupMember: Pick<IGroupMember, 'id'>): number {
    return groupMember.id;
  }

  compareGroupMember(o1: Pick<IGroupMember, 'id'> | null, o2: Pick<IGroupMember, 'id'> | null): boolean {
    return o1 && o2 ? this.getGroupMemberIdentifier(o1) === this.getGroupMemberIdentifier(o2) : o1 === o2;
  }

  addGroupMemberToCollectionIfMissing<Type extends Pick<IGroupMember, 'id'>>(
    groupMemberCollection: Type[],
    ...groupMembersToCheck: (Type | null | undefined)[]
  ): Type[] {
    const groupMembers: Type[] = groupMembersToCheck.filter(isPresent);
    if (groupMembers.length > 0) {
      const groupMemberCollectionIdentifiers = groupMemberCollection.map(groupMemberItem => this.getGroupMemberIdentifier(groupMemberItem));
      const groupMembersToAdd = groupMembers.filter(groupMemberItem => {
        const groupMemberIdentifier = this.getGroupMemberIdentifier(groupMemberItem);
        if (groupMemberCollectionIdentifiers.includes(groupMemberIdentifier)) {
          return false;
        }
        groupMemberCollectionIdentifiers.push(groupMemberIdentifier);
        return true;
      });
      return [...groupMembersToAdd, ...groupMemberCollection];
    }
    return groupMemberCollection;
  }
}
