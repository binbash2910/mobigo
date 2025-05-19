import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable, asapScheduler, map, scheduled } from 'rxjs';

import { catchError } from 'rxjs/operators';

import dayjs from 'dayjs/esm';

import { isPresent } from 'app/core/util/operators';
import { DATE_FORMAT } from 'app/config/input.constants';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import { Search } from 'app/core/request/request.model';
import { IPeople, NewPeople } from '../people.model';

export type PartialUpdatePeople = Partial<IPeople> & Pick<IPeople, 'id'>;

type RestOf<T extends IPeople | NewPeople> = Omit<T, 'dateNaissance'> & {
  dateNaissance?: string | null;
};

export type RestPeople = RestOf<IPeople>;

export type NewRestPeople = RestOf<NewPeople>;

export type PartialUpdateRestPeople = RestOf<PartialUpdatePeople>;

export type EntityResponseType = HttpResponse<IPeople>;
export type EntityArrayResponseType = HttpResponse<IPeople[]>;

@Injectable({ providedIn: 'root' })
export class PeopleService {
  protected readonly http = inject(HttpClient);
  protected readonly applicationConfigService = inject(ApplicationConfigService);

  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/people');
  protected resourceSearchUrl = this.applicationConfigService.getEndpointFor('api/people/_search');

  create(people: NewPeople): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(people);
    return this.http
      .post<RestPeople>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  update(people: IPeople): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(people);
    return this.http
      .put<RestPeople>(`${this.resourceUrl}/${this.getPeopleIdentifier(people)}`, copy, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  partialUpdate(people: PartialUpdatePeople): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(people);
    return this.http
      .patch<RestPeople>(`${this.resourceUrl}/${this.getPeopleIdentifier(people)}`, copy, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http
      .get<RestPeople>(`${this.resourceUrl}/${id}`, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<RestPeople[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map(res => this.convertResponseArrayFromServer(res)));
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  search(req: Search): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<RestPeople[]>(this.resourceSearchUrl, { params: options, observe: 'response' }).pipe(
      map(res => this.convertResponseArrayFromServer(res)),

      catchError(() => scheduled([new HttpResponse<IPeople[]>()], asapScheduler)),
    );
  }

  getPeopleIdentifier(people: Pick<IPeople, 'id'>): number {
    return people.id;
  }

  comparePeople(o1: Pick<IPeople, 'id'> | null, o2: Pick<IPeople, 'id'> | null): boolean {
    return o1 && o2 ? this.getPeopleIdentifier(o1) === this.getPeopleIdentifier(o2) : o1 === o2;
  }

  addPeopleToCollectionIfMissing<Type extends Pick<IPeople, 'id'>>(
    peopleCollection: Type[],
    ...peopleToCheck: (Type | null | undefined)[]
  ): Type[] {
    const people: Type[] = peopleToCheck.filter(isPresent);
    if (people.length > 0) {
      const peopleCollectionIdentifiers = peopleCollection.map(peopleItem => this.getPeopleIdentifier(peopleItem));
      const peopleToAdd = people.filter(peopleItem => {
        const peopleIdentifier = this.getPeopleIdentifier(peopleItem);
        if (peopleCollectionIdentifiers.includes(peopleIdentifier)) {
          return false;
        }
        peopleCollectionIdentifiers.push(peopleIdentifier);
        return true;
      });
      return [...peopleToAdd, ...peopleCollection];
    }
    return peopleCollection;
  }

  protected convertDateFromClient<T extends IPeople | NewPeople | PartialUpdatePeople>(people: T): RestOf<T> {
    return {
      ...people,
      dateNaissance: people.dateNaissance?.format(DATE_FORMAT) ?? null,
    };
  }

  protected convertDateFromServer(restPeople: RestPeople): IPeople {
    return {
      ...restPeople,
      dateNaissance: restPeople.dateNaissance ? dayjs(restPeople.dateNaissance) : undefined,
    };
  }

  protected convertResponseFromServer(res: HttpResponse<RestPeople>): HttpResponse<IPeople> {
    return res.clone({
      body: res.body ? this.convertDateFromServer(res.body) : null,
    });
  }

  protected convertResponseArrayFromServer(res: HttpResponse<RestPeople[]>): HttpResponse<IPeople[]> {
    return res.clone({
      body: res.body ? res.body.map(item => this.convertDateFromServer(item)) : null,
    });
  }
}
