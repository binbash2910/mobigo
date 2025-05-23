import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable, asapScheduler, scheduled } from 'rxjs';

import { catchError } from 'rxjs/operators';

import { isPresent } from 'app/core/util/operators';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import { Search } from 'app/core/request/request.model';
import { IGroupAuthority, NewGroupAuthority } from '../group-authority.model';

export type PartialUpdateGroupAuthority = Partial<IGroupAuthority> & Pick<IGroupAuthority, 'id'>;

export type EntityResponseType = HttpResponse<IGroupAuthority>;
export type EntityArrayResponseType = HttpResponse<IGroupAuthority[]>;

@Injectable({ providedIn: 'root' })
export class GroupAuthorityService {
  protected readonly http = inject(HttpClient);
  protected readonly applicationConfigService = inject(ApplicationConfigService);

  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/group-authorities');
  protected resourceSearchUrl = this.applicationConfigService.getEndpointFor('api/group-authorities/_search');

  create(groupAuthority: NewGroupAuthority): Observable<EntityResponseType> {
    return this.http.post<IGroupAuthority>(this.resourceUrl, groupAuthority, { observe: 'response' });
  }

  update(groupAuthority: IGroupAuthority): Observable<EntityResponseType> {
    return this.http.put<IGroupAuthority>(`${this.resourceUrl}/${this.getGroupAuthorityIdentifier(groupAuthority)}`, groupAuthority, {
      observe: 'response',
    });
  }

  partialUpdate(groupAuthority: PartialUpdateGroupAuthority): Observable<EntityResponseType> {
    return this.http.patch<IGroupAuthority>(`${this.resourceUrl}/${this.getGroupAuthorityIdentifier(groupAuthority)}`, groupAuthority, {
      observe: 'response',
    });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IGroupAuthority>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IGroupAuthority[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  search(req: Search): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<IGroupAuthority[]>(this.resourceSearchUrl, { params: options, observe: 'response' })
      .pipe(catchError(() => scheduled([new HttpResponse<IGroupAuthority[]>()], asapScheduler)));
  }

  getGroupAuthorityIdentifier(groupAuthority: Pick<IGroupAuthority, 'id'>): number {
    return groupAuthority.id;
  }

  compareGroupAuthority(o1: Pick<IGroupAuthority, 'id'> | null, o2: Pick<IGroupAuthority, 'id'> | null): boolean {
    return o1 && o2 ? this.getGroupAuthorityIdentifier(o1) === this.getGroupAuthorityIdentifier(o2) : o1 === o2;
  }

  addGroupAuthorityToCollectionIfMissing<Type extends Pick<IGroupAuthority, 'id'>>(
    groupAuthorityCollection: Type[],
    ...groupAuthoritiesToCheck: (Type | null | undefined)[]
  ): Type[] {
    const groupAuthorities: Type[] = groupAuthoritiesToCheck.filter(isPresent);
    if (groupAuthorities.length > 0) {
      const groupAuthorityCollectionIdentifiers = groupAuthorityCollection.map(groupAuthorityItem =>
        this.getGroupAuthorityIdentifier(groupAuthorityItem),
      );
      const groupAuthoritiesToAdd = groupAuthorities.filter(groupAuthorityItem => {
        const groupAuthorityIdentifier = this.getGroupAuthorityIdentifier(groupAuthorityItem);
        if (groupAuthorityCollectionIdentifiers.includes(groupAuthorityIdentifier)) {
          return false;
        }
        groupAuthorityCollectionIdentifiers.push(groupAuthorityIdentifier);
        return true;
      });
      return [...groupAuthoritiesToAdd, ...groupAuthorityCollection];
    }
    return groupAuthorityCollection;
  }
}
