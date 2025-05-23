import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { ASC } from 'app/config/navigation.constants';
import GroupAuthorityResolve from './route/group-authority-routing-resolve.service';

const groupAuthorityRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/group-authority.component').then(m => m.GroupAuthorityComponent),
    data: {
      defaultSort: `id,${ASC}`,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    loadComponent: () => import('./detail/group-authority-detail.component').then(m => m.GroupAuthorityDetailComponent),
    resolve: {
      groupAuthority: GroupAuthorityResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    loadComponent: () => import('./update/group-authority-update.component').then(m => m.GroupAuthorityUpdateComponent),
    resolve: {
      groupAuthority: GroupAuthorityResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./update/group-authority-update.component').then(m => m.GroupAuthorityUpdateComponent),
    resolve: {
      groupAuthority: GroupAuthorityResolve,
    },
    canActivate: [UserRouteAccessService],
  },
];

export default groupAuthorityRoute;
