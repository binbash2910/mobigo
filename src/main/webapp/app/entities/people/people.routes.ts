import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { ASC } from 'app/config/navigation.constants';
import PeopleResolve from './route/people-routing-resolve.service';

const peopleRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/people.component').then(m => m.PeopleComponent),
    data: {
      defaultSort: `id,${ASC}`,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    loadComponent: () => import('./detail/people-detail.component').then(m => m.PeopleDetailComponent),
    resolve: {
      people: PeopleResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    loadComponent: () => import('./update/people-update.component').then(m => m.PeopleUpdateComponent),
    resolve: {
      people: PeopleResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./update/people-update.component').then(m => m.PeopleUpdateComponent),
    resolve: {
      people: PeopleResolve,
    },
    canActivate: [UserRouteAccessService],
  },
];

export default peopleRoute;
