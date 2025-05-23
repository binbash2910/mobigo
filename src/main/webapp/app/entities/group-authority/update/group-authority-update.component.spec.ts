import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpResponse, provideHttpClient } from '@angular/common/http';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Subject, from, of } from 'rxjs';

import { IAuthority } from 'app/entities/admin/authority/authority.model';
import { AuthorityService } from 'app/entities/admin/authority/service/authority.service';
import { IGroup } from 'app/entities/group/group.model';
import { GroupService } from 'app/entities/group/service/group.service';
import { IGroupAuthority } from '../group-authority.model';
import { GroupAuthorityService } from '../service/group-authority.service';
import { GroupAuthorityFormService } from './group-authority-form.service';

import { GroupAuthorityUpdateComponent } from './group-authority-update.component';

describe('GroupAuthority Management Update Component', () => {
  let comp: GroupAuthorityUpdateComponent;
  let fixture: ComponentFixture<GroupAuthorityUpdateComponent>;
  let activatedRoute: ActivatedRoute;
  let groupAuthorityFormService: GroupAuthorityFormService;
  let groupAuthorityService: GroupAuthorityService;
  let authorityService: AuthorityService;
  let groupService: GroupService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GroupAuthorityUpdateComponent],
      providers: [
        provideHttpClient(),
        FormBuilder,
        {
          provide: ActivatedRoute,
          useValue: {
            params: from([{}]),
          },
        },
      ],
    })
      .overrideTemplate(GroupAuthorityUpdateComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(GroupAuthorityUpdateComponent);
    activatedRoute = TestBed.inject(ActivatedRoute);
    groupAuthorityFormService = TestBed.inject(GroupAuthorityFormService);
    groupAuthorityService = TestBed.inject(GroupAuthorityService);
    authorityService = TestBed.inject(AuthorityService);
    groupService = TestBed.inject(GroupService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('should call Authority query and add missing value', () => {
      const groupAuthority: IGroupAuthority = { id: 24812 };
      const authority: IAuthority = { name: '572a7ecc-bf76-43f4-8026-46b42fba586d' };
      groupAuthority.authority = authority;

      const authorityCollection: IAuthority[] = [{ name: '572a7ecc-bf76-43f4-8026-46b42fba586d' }];
      jest.spyOn(authorityService, 'query').mockReturnValue(of(new HttpResponse({ body: authorityCollection })));
      const additionalAuthorities = [authority];
      const expectedCollection: IAuthority[] = [...additionalAuthorities, ...authorityCollection];
      jest.spyOn(authorityService, 'addAuthorityToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ groupAuthority });
      comp.ngOnInit();

      expect(authorityService.query).toHaveBeenCalled();
      expect(authorityService.addAuthorityToCollectionIfMissing).toHaveBeenCalledWith(
        authorityCollection,
        ...additionalAuthorities.map(expect.objectContaining),
      );
      expect(comp.authoritiesSharedCollection).toEqual(expectedCollection);
    });

    it('should call Group query and add missing value', () => {
      const groupAuthority: IGroupAuthority = { id: 24812 };
      const group: IGroup = { id: 25136 };
      groupAuthority.group = group;

      const groupCollection: IGroup[] = [{ id: 25136 }];
      jest.spyOn(groupService, 'query').mockReturnValue(of(new HttpResponse({ body: groupCollection })));
      const additionalGroups = [group];
      const expectedCollection: IGroup[] = [...additionalGroups, ...groupCollection];
      jest.spyOn(groupService, 'addGroupToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ groupAuthority });
      comp.ngOnInit();

      expect(groupService.query).toHaveBeenCalled();
      expect(groupService.addGroupToCollectionIfMissing).toHaveBeenCalledWith(
        groupCollection,
        ...additionalGroups.map(expect.objectContaining),
      );
      expect(comp.groupsSharedCollection).toEqual(expectedCollection);
    });

    it('should update editForm', () => {
      const groupAuthority: IGroupAuthority = { id: 24812 };
      const authority: IAuthority = { name: '572a7ecc-bf76-43f4-8026-46b42fba586d' };
      groupAuthority.authority = authority;
      const group: IGroup = { id: 25136 };
      groupAuthority.group = group;

      activatedRoute.data = of({ groupAuthority });
      comp.ngOnInit();

      expect(comp.authoritiesSharedCollection).toContainEqual(authority);
      expect(comp.groupsSharedCollection).toContainEqual(group);
      expect(comp.groupAuthority).toEqual(groupAuthority);
    });
  });

  describe('save', () => {
    it('should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IGroupAuthority>>();
      const groupAuthority = { id: 1264 };
      jest.spyOn(groupAuthorityFormService, 'getGroupAuthority').mockReturnValue(groupAuthority);
      jest.spyOn(groupAuthorityService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ groupAuthority });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: groupAuthority }));
      saveSubject.complete();

      // THEN
      expect(groupAuthorityFormService.getGroupAuthority).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(groupAuthorityService.update).toHaveBeenCalledWith(expect.objectContaining(groupAuthority));
      expect(comp.isSaving).toEqual(false);
    });

    it('should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IGroupAuthority>>();
      const groupAuthority = { id: 1264 };
      jest.spyOn(groupAuthorityFormService, 'getGroupAuthority').mockReturnValue({ id: null });
      jest.spyOn(groupAuthorityService, 'create').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ groupAuthority: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: groupAuthority }));
      saveSubject.complete();

      // THEN
      expect(groupAuthorityFormService.getGroupAuthority).toHaveBeenCalled();
      expect(groupAuthorityService.create).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IGroupAuthority>>();
      const groupAuthority = { id: 1264 };
      jest.spyOn(groupAuthorityService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ groupAuthority });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(groupAuthorityService.update).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });

  describe('Compare relationships', () => {
    describe('compareAuthority', () => {
      it('should forward to authorityService', () => {
        const entity = { name: '572a7ecc-bf76-43f4-8026-46b42fba586d' };
        const entity2 = { name: 'c56c1cf7-aca8-48fe-ad81-eeebbf872cb1' };
        jest.spyOn(authorityService, 'compareAuthority');
        comp.compareAuthority(entity, entity2);
        expect(authorityService.compareAuthority).toHaveBeenCalledWith(entity, entity2);
      });
    });

    describe('compareGroup', () => {
      it('should forward to groupService', () => {
        const entity = { id: 25136 };
        const entity2 = { id: 19049 };
        jest.spyOn(groupService, 'compareGroup');
        comp.compareGroup(entity, entity2);
        expect(groupService.compareGroup).toHaveBeenCalledWith(entity, entity2);
      });
    });
  });
});
