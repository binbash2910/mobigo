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
import { IRide, NewRide } from '../ride.model';

export type PartialUpdateRide = Partial<IRide> & Pick<IRide, 'id'>;

type RestOf<T extends IRide | NewRide> = Omit<T, 'dateDepart' | 'dateArrivee'> & {
  dateDepart?: string | null;
  dateArrivee?: string | null;
};

export type RestRide = RestOf<IRide>;

export type NewRestRide = RestOf<NewRide>;

export type PartialUpdateRestRide = RestOf<PartialUpdateRide>;

export type EntityResponseType = HttpResponse<IRide>;
export type EntityArrayResponseType = HttpResponse<IRide[]>;

@Injectable({ providedIn: 'root' })
export class RideService {
  protected readonly http = inject(HttpClient);
  protected readonly applicationConfigService = inject(ApplicationConfigService);

  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/rides');
  protected resourceSearchUrl = this.applicationConfigService.getEndpointFor('api/rides/_search');

  create(ride: NewRide): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(ride);
    return this.http.post<RestRide>(this.resourceUrl, copy, { observe: 'response' }).pipe(map(res => this.convertResponseFromServer(res)));
  }

  update(ride: IRide): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(ride);
    return this.http
      .put<RestRide>(`${this.resourceUrl}/${this.getRideIdentifier(ride)}`, copy, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  partialUpdate(ride: PartialUpdateRide): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(ride);
    return this.http
      .patch<RestRide>(`${this.resourceUrl}/${this.getRideIdentifier(ride)}`, copy, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http
      .get<RestRide>(`${this.resourceUrl}/${id}`, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<RestRide[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map(res => this.convertResponseArrayFromServer(res)));
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  search(req: Search): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<RestRide[]>(this.resourceSearchUrl, { params: options, observe: 'response' }).pipe(
      map(res => this.convertResponseArrayFromServer(res)),

      catchError(() => scheduled([new HttpResponse<IRide[]>()], asapScheduler)),
    );
  }

  getRideIdentifier(ride: Pick<IRide, 'id'>): number {
    return ride.id;
  }

  compareRide(o1: Pick<IRide, 'id'> | null, o2: Pick<IRide, 'id'> | null): boolean {
    return o1 && o2 ? this.getRideIdentifier(o1) === this.getRideIdentifier(o2) : o1 === o2;
  }

  addRideToCollectionIfMissing<Type extends Pick<IRide, 'id'>>(
    rideCollection: Type[],
    ...ridesToCheck: (Type | null | undefined)[]
  ): Type[] {
    const rides: Type[] = ridesToCheck.filter(isPresent);
    if (rides.length > 0) {
      const rideCollectionIdentifiers = rideCollection.map(rideItem => this.getRideIdentifier(rideItem));
      const ridesToAdd = rides.filter(rideItem => {
        const rideIdentifier = this.getRideIdentifier(rideItem);
        if (rideCollectionIdentifiers.includes(rideIdentifier)) {
          return false;
        }
        rideCollectionIdentifiers.push(rideIdentifier);
        return true;
      });
      return [...ridesToAdd, ...rideCollection];
    }
    return rideCollection;
  }

  protected convertDateFromClient<T extends IRide | NewRide | PartialUpdateRide>(ride: T): RestOf<T> {
    return {
      ...ride,
      dateDepart: ride.dateDepart?.format(DATE_FORMAT) ?? null,
      dateArrivee: ride.dateArrivee?.format(DATE_FORMAT) ?? null,
    };
  }

  protected convertDateFromServer(restRide: RestRide): IRide {
    return {
      ...restRide,
      dateDepart: restRide.dateDepart ? dayjs(restRide.dateDepart) : undefined,
      dateArrivee: restRide.dateArrivee ? dayjs(restRide.dateArrivee) : undefined,
    };
  }

  protected convertResponseFromServer(res: HttpResponse<RestRide>): HttpResponse<IRide> {
    return res.clone({
      body: res.body ? this.convertDateFromServer(res.body) : null,
    });
  }

  protected convertResponseArrayFromServer(res: HttpResponse<RestRide[]>): HttpResponse<IRide[]> {
    return res.clone({
      body: res.body ? res.body.map(item => this.convertDateFromServer(item)) : null,
    });
  }
}
